from io import BytesIO

from django.http import HttpResponse
from openpyxl import Workbook
from rest_framework.views import APIView

from .auth import EsAdministrador

COLUMNAS = ['Nombres', 'Apellidos', 'Correo', 'Curso', 'Sección']
TIPO_XLSX = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'

INSTRUCCIONES = [
    'Complete una fila por alumno en la hoja "Alumnos" y no cambie los títulos de las columnas.',
    'Nombres y Apellidos: obligatorios.',
    'Correo: obligatorio y único. Será el usuario con el que el alumno ingresa.',
    'Curso y Sección: deben coincidir con un curso y una sección ya registrados y activos.',
    'Cada alumno recibirá una contraseña temporal por correo y deberá cambiarla al ingresar.',
    'Borre la fila de ejemplo antes de cargar el archivo.',
]


def generar_plantilla():
    libro = Workbook()
    hoja = libro.active
    hoja.title = 'Alumnos'
    hoja.append(COLUMNAS)
    hoja.append(['María', 'Quispe Huamán', 'maria.quispe@correo.com', 'Matemática', 'A'])
    for columna, ancho in zip('ABCDE', (22, 26, 34, 24, 12)):
        hoja.column_dimensions[columna].width = ancho
    ayuda = libro.create_sheet('Instrucciones')
    for linea in INSTRUCCIONES:
        ayuda.append([linea])
    ayuda.column_dimensions['A'].width = 100
    salida = BytesIO()
    libro.save(salida)
    return salida.getvalue()


class PlantillaAlumnosView(APIView):
    permission_classes = [EsAdministrador]

    def get(self, request):
        respuesta = HttpResponse(generar_plantilla(), content_type=TIPO_XLSX)
        respuesta['Content-Disposition'] = 'attachment; filename="plantilla_alumnos.xlsx"'
        return respuesta
