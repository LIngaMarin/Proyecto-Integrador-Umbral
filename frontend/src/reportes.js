import { api, esc, query } from './api.js';
import { icon } from './icons.js';

// HU41: las cifras proceden de la consulta autorizada; no se calculan notas de reemplazo en el navegador.
const vacio=message=>`<div class="empty">${esc(message)}</div>`;
const numero=value=>value==null?'Sin datos':new Intl.NumberFormat('es-PE',{minimumFractionDigits:2,maximumFractionDigits:2}).format(value);
const opciones=(rows,selected='')=>`<option value="">Todos los simulacros</option>${rows.map(s=>`<option value="${s.id}" ${String(s.id)===String(selected)?'selected':''}>${esc(s.nombre || `Simulacro #${s.id}`)}${s.fecha?` · ${esc(s.fecha)}`:''}</option>`).join('')}`;

/** Presenta la misma selección, detalle e indicadores que utiliza la plantilla PDF del servidor. */
function contenido(data){
  const r=data.resumen;
  const metrics=[['Resultados registrados',r.resultados],['Alumnos con resultados',r.alumnos],['Promedio por resultado',numero(r.promedio)],['Puntajes mínimo / máximo',`${numero(r.minimo)} / ${numero(r.maximo)}`]];
  return `<div class="report-context"><h2>${esc(data.seccionNombre)}</h2><p class="muted">Docente: ${esc(data.docenteEmail)}</p><p class="muted small">Consulta: ${esc(new Date(data.generadoEn).toLocaleString('es-PE',{timeZone:'America/Lima'}))} · Lima</p></div><div class="cards report-metrics">${metrics.map(([label,value])=>`<article class="panel"><p class="eyebrow">${label}</p><div class="metric">${esc(value)}</div></article>`).join('')}</div><article class="panel"><h2>Resultados de rendimiento</h2>${data.resultados.length?`<div class="table-wrap"><table class="table align-middle"><caption>Resultados registrados para la sección y el simulacro seleccionados.</caption><thead><tr><th scope="col">Alumno</th><th scope="col">Simulacro</th><th scope="col">Puntaje</th><th scope="col">Temas por reforzar</th></tr></thead><tbody>${data.resultados.map(f=>`<tr><td>${esc(f.alumno || `Alumno #${f.alumnoId}`)}</td><td>${esc(f.simulacro || `Simulacro #${f.simulacroId}`)}</td><td>${numero(f.puntaje)}</td><td>${esc(f.temasPorReforzar.join(', ') || 'Sin errores registrados')}</td></tr>`).join('')}</tbody></table></div>`:vacio('No hay resultados registrados para esta selección. Puedes exportar el reporte con este estado.')}</article><article class="panel"><h2>Temas que requieren refuerzo</h2>${data.temas.length?`<div class="table-wrap"><table class="table"><thead><tr><th scope="col">Tema</th><th scope="col">Errores registrados</th></tr></thead><tbody>${data.temas.map(t=>`<tr><td>${esc(t.tema)}</td><td>${esc(t.cantidadErrores)}</td></tr>`).join('')}</tbody></table></div>`:vacio('No hay temas incorrectos registrados para esta selección.')}</article>`;
}

/** La pantalla solo ofrece secciones devueltas por /cuenta; el backend vuelve a autorizar cada lectura. */
export async function reportes(account){
  if(account.rol!=='DOCENTE')return vacio('Esta sección está disponible para docentes.');
  const heading='<p class="eyebrow">Coordinación académica</p><h1>Reportes académicos</h1><p class="muted">Consulta el rendimiento de tu sección y comparte un reporte académico.</p>';
  if(!account.secciones.length)return heading+vacio('No tienes secciones asignadas para consultar reportes.');
  const initial=await api(`/docente/secciones/${account.secciones[0].id}/rendimiento`);
  // Los formularios y el resultado están separados para no perder el filtro al actualizar la tabla.
  return `${heading}<!-- Filtros autorizados y exportación del reporte académico. --><article class="panel"><form id="report-filters" class="filters"><label>Sección académica<select class="form-select" name="seccion">${account.secciones.map(s=>`<option value="${s.id}">${esc(s.nombre)}</option>`).join('')}</select></label><label>Simulacro<select class="form-select" name="simulacro">${opciones(initial.simulacros)}</select></label><button class="btn btn-outline-secondary" type="submit">Consultar resultados</button><button class="btn btn-primary" id="export-report" type="button">${icon('chart')} Exportar PDF</button></form><div id="report-status" class="mt-3" role="status" aria-live="polite"></div></article><!-- Indicadores y tablas obtenidos del backend para la selección actual. --><section id="report-preview" aria-live="polite">${contenido(initial)}</section>`;
}

/** Vincula consulta y exportación; cancela lecturas antiguas y bloquea descargar una selección desactualizada. */
export function bindReportes(account){
  const form=document.querySelector('#report-filters');
  if(!form)return;
  const preview=document.querySelector('#report-preview');
  const status=document.querySelector('#report-status');
  const download=form.querySelector('#export-report');
  let version=0;
  let abort;
  let valid=true;
  const values=()=>({seccion:form.elements.seccion.value,simulacro:form.elements.simulacro.value});
  const route=(v,pdf=false)=>`/docente/secciones/${v.seccion}/rendimiento${pdf?'.pdf':''}?${query({simulacroId:v.simulacro})}`;
  const busy=(active)=>{form.querySelectorAll('button,select').forEach(e=>{e.disabled=active;});preview.setAttribute('aria-busy',String(active));};
  const error=message=>{status.innerHTML=`<p class="text-danger" role="alert">${esc(message)}</p>`;};
  async function consultar(){
    const current=++version;
    abort?.abort();abort=new AbortController();
    const selected=values();
    if(!account.secciones.some(s=>String(s.id)===selected.seccion)){error('Selecciona una sección asignada.');return;}
    valid=false;busy(true);status.textContent='Consultando resultados…';preview.innerHTML='';
    try{
      const data=await api(route(selected),{signal:AbortSignal.any([abort.signal,AbortSignal.timeout(15000)])});
      if(current!==version || !form.isConnected)return;
      form.elements.simulacro.innerHTML=opciones(data.simulacros,selected.simulacro);
      preview.innerHTML=contenido(data);valid=true;status.textContent='Resultados actualizados.';
    }catch(ex){if(current===version && form.isConnected){preview.innerHTML=vacio('No se pudieron consultar los resultados. Vuelve a intentarlo.');error(ex.message);}}
    finally{if(current===version && form.isConnected){busy(false);download.disabled=!valid;}}
  }
  form.onsubmit=event=>{event.preventDefault();consultar();};
  form.elements.seccion.onchange=()=>{form.elements.simulacro.value='';consultar();};
  form.elements.simulacro.onchange=consultar;
  download.onclick=async()=>{
    if(!valid)return;
    busy(true);download.innerHTML='Generando PDF…';status.textContent='Preparando el reporte académico…';
    try{
      const selected=values();
      const blob=await api(route(selected,true),{blob:true});
      if(!form.isConnected)return;
      if(!blob.type.includes('application/pdf') || blob.size===0)throw new Error('No se recibió un reporte válido. Vuelve a intentarlo.');
      const url=URL.createObjectURL(blob);
      const a=document.createElement('a');a.href=url;a.download=`rendimiento-seccion-${selected.seccion}.pdf`;
      document.body.append(a);a.click();a.remove();setTimeout(()=>URL.revokeObjectURL(url),5000);
      if(form.isConnected)status.textContent='Reporte preparado para descargar.';
    }catch(ex){if(form.isConnected)error(ex.message);}
    finally{if(form.isConnected){busy(false);download.innerHTML=`${icon('chart')} Exportar PDF`;}}
  };
}
