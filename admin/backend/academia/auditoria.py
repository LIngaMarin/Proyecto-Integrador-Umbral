import logging

from rest_framework_simplejwt.authentication import JWTAuthentication

from .models import RegistroAuditoria

logger = logging.getLogger(__name__)

ACCIONES = {
    'POST': RegistroAuditoria.Accion.CREAR,
    'PUT': RegistroAuditoria.Accion.EDITAR,
    'PATCH': RegistroAuditoria.Accion.EDITAR,
    'DELETE': RegistroAuditoria.Accion.ELIMINAR,
}

PREFIJO_API = '/api/'
# Rutas de escritura que no son un cambio de datos del panel: el inicio de
# sesión y la renovación del token, y el consumo de IA (que ya tiene su propio
# registro de uso).
RUTAS_EXCLUIDAS = ('auth/', 'ia/usar/')


def separar_ruta(ruta):
    """Devuelve (recurso, objeto_id) a partir de una ruta bajo /api/.

    '/api/alumnos/5/' -> ('alumnos', '5');  '/api/alumnos-masivo/cargar/' -> ('alumnos-masivo', '')
    """
    segmentos = [s for s in ruta[len(PREFIJO_API):].split('/') if s]
    recurso = segmentos[0] if segmentos else ''
    objeto_id = next((s for s in segmentos[1:] if s.isdigit()), '')
    return recurso, objeto_id


def debe_registrarse(request, response):
    if request.method not in ACCIONES or response.status_code >= 400:
        return False
    if not request.path.startswith(PREFIJO_API):
        return False
    return not request.path[len(PREFIJO_API):].startswith(RUTAS_EXCLUIDAS)


def ip_del_cliente(request):
    reenviada = request.META.get('HTTP_X_FORWARDED_FOR', '')
    ip = reenviada.split(',')[0].strip() if reenviada else request.META.get('REMOTE_ADDR', '')
    return ip or None


def registrar(request, response):
    try:
        autenticado = JWTAuthentication().authenticate(request)
    except Exception:
        autenticado = None
    if autenticado is None:
        return
    usuario = autenticado[0]
    perfil = getattr(usuario, 'perfil', None)
    recurso, objeto_id = separar_ruta(request.path)
    RegistroAuditoria.objects.create(
        usuario=usuario,
        usuario_nombre=usuario.get_full_name() or usuario.username,
        rol=perfil.rol if perfil else '',
        accion=ACCIONES[request.method],
        recurso=recurso[:60],
        objeto_id=objeto_id[:20],
        metodo=request.method,
        ruta=request.path[:255],
        estado=response.status_code,
        ip=ip_del_cliente(request),
    )


class AuditoriaMiddleware:
    """Registra cada escritura exitosa (crear, editar, eliminar) hecha con sesión iniciada.

    Un fallo al registrar nunca debe romper la respuesta al usuario.
    """

    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        response = self.get_response(request)
        if debe_registrarse(request, response):
            try:
                registrar(request, response)
            except Exception:
                logger.exception('No se pudo registrar la auditoría de %s', request.path)
        return response
