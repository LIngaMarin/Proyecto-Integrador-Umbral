import { Carousel } from 'bootstrap';
import { icon } from './icons.js';

/** Portada de preparación universitaria: todos los accesos llevan al catálogo o a un panel autenticado. */
export function landing(root) {
  const slides = [
    { image: 'estudiantes', alt: 'Estudiantes preparando juntos sus cursos', label: 'Tu camino a la universidad', title: 'Tu próximo objetivo<br>comienza aquí.', text: 'Dale dirección a tu esfuerzo. Prepárate, aprende y construye tu siguiente paso.', action: 'Comenzar mi preparación', href: '#acceso' },
    { image: 'estudio', alt: 'Material de estudio y apuntes para practicar', label: 'Una pregunta a la vez', title: 'La constancia<br>se entrena.', text: 'Elige un tema, resuelve preguntas y aprende de cada respuesta.', action: 'Explorar prácticas', href: '#practicas' },
    { image: 'aula', alt: 'Estudiantes aprendiendo con acompañamiento docente en el aula', label: 'Aprende con orientación', title: 'Pregunta. Comprende.<br>Avanza.', text: 'Comparte tus dudas y encuentra orientación para seguir aprendiendo.', action: 'Consultar a mi docente', href: '#consultas' },
  ];
  root.innerHTML = `<div class="public-page"><header class="public-header"><a class="wordmark" href="#portada" aria-label="UMBRAL, inicio">${icon('cap')} UMBRAL</a><button class="mobile-menu btn btn-outline-secondary" type="button" aria-expanded="false" aria-controls="public-nav">Menú</button><nav id="public-nav" aria-label="Navegación principal"><a href="#portada" aria-current="page">Inicio</a><a href="#herramientas">Herramientas</a><a href="#practicas">Prácticas</a><a class="btn btn-primary" href="#acceso">Iniciar sesión ${icon('arrow')}</a></nav></header>
    <main class="public-main"><section class="welcome-line"><span>PREPARACIÓN PREUNIVERSITARIA</span><span>Un paso más cerca de tu meta ${icon('arrow')}</span></section>
    <section id="academic-carousel" class="carousel slide carousel-fade" role="region" aria-roledescription="carrusel" aria-label="Preparación académica"><div class="carousel-inner">${slides.map((s,i) => `<article class="carousel-item ${i===0?'active':''}" role="group" aria-roledescription="diapositiva" aria-label="${i+1} de 3"><img src="/images/${s.image}.webp" alt="${s.alt}" width="1280" height="740" ${i===0?'fetchpriority="high"':'loading="lazy"'}><div class="slide-overlay"></div><div class="slide-copy"><p class="eyebrow">${s.label}</p><${i===0?'h1':'h2'}>${s.title}</${i===0?'h1':'h2'}><p>${s.text}</p><a class="btn btn-light" href="${s.href}">${s.action} ${icon('arrow')}</a></div></article>`).join('')}</div>
    <div class="carousel-bottom"><div class="carousel-indicators">${slides.map((s,i) => `<button type="button" data-bs-target="#academic-carousel" data-bs-slide-to="${i}" class="${i===0?'active':''}" ${i===0?'aria-current="true"':''} aria-label="Ir a diapositiva ${i+1}"></button>`).join('')}</div><div class="carousel-tools"><button class="carousel-pause" type="button" aria-label="Pausar carrusel" aria-pressed="false">Pausar</button><button class="carousel-prev" type="button" data-bs-target="#academic-carousel" data-bs-slide="prev" aria-label="Diapositiva anterior">←</button><button class="carousel-next" type="button" data-bs-target="#academic-carousel" data-bs-slide="next" aria-label="Diapositiva siguiente">→</button></div></div></section>
    <section id="herramientas" class="tool-section"><div class="section-intro"><div><p class="eyebrow">Dale impulso a tu preparación</p><h2>Aprender también es avanzar.</h2></div><p>Herramientas para convertir<br>tu esfuerzo en aprendizaje.</p></div><div class="tool-grid">${[
      ['book','Practica a tu ritmo','Preguntas por tema y dificultad para reforzar lo aprendido.','Explorar preguntas','#practicas'],
      ['chat','Resuelve tus dudas','Pregunta a tus docentes y revisa sus respuestas.','Ir a mis consultas','#consultas'],
      ['chart','Reconoce tu progreso','Revisa tus respuestas y encuentra qué puedes mejorar.','Ver mis resultados','#resultados'],
    ].map(([symbol,title,text,action,href],i) => `<article class="tool-card"><div class="tool-top"><span class="tool-icon">${icon(symbol)}</span><span>0${i+1}</span></div><h3>${title}</h3><p>${text}</p><a href="${href}">${action} ${icon('arrow')}</a></article>`).join('')}</div></section>
    <section class="study-banner"><span class="tool-icon">${icon('cap')}</span><div><p class="eyebrow">Haz espacio para tu futuro</p><h2>Hoy puedes dar el siguiente paso.</h2><p>Retoma tus prácticas y sigue preparando tu ingreso.</p></div><a class="btn btn-primary" href="#acceso">Entrar a mi campus ${icon('arrow')}</a></section></main>
    <footer class="public-footer"><a class="wordmark" href="#portada">UMBRAL</a><p>Preparación, aprendizaje y nuevos objetivos.</p><a href="#acceso">Acceso académico ${icon('arrow')}</a><small>© ${new Date().getFullYear()} UMBRAL</small></footer></div>`;
  document.title='UMBRAL · Tu camino a la universidad';
  const menu=root.querySelector('.mobile-menu');
  menu.onclick=()=>{ const expanded=menu.getAttribute('aria-expanded')==='true'; menu.setAttribute('aria-expanded',String(!expanded)); root.querySelector('#public-nav').classList.toggle('open',!expanded); };
  const element=root.querySelector('#academic-carousel');
  const reduced=window.matchMedia('(prefers-reduced-motion: reduce)');
  let paused=reduced.matches;
  const carousel=new Carousel(element,{interval:6000,ride:reduced.matches?false:'carousel',pause:'hover',touch:true,keyboard:true});
  const pause=root.querySelector('.carousel-pause');
  const update=()=>{ pause.setAttribute('aria-pressed',String(paused)); pause.setAttribute('aria-label',paused?'Reanudar carrusel':'Pausar carrusel'); pause.textContent=paused?'Reanudar':'Pausar'; if(paused||reduced.matches)carousel.pause();else carousel.cycle(); };
  pause.onclick=()=>{paused=!paused;update();};
  // El teclado pausa la rotación para que el contenido no cambie mientras se exploran sus enlaces.
  element.addEventListener('focusin',()=>carousel.pause());
  element.addEventListener('focusout',event=>{if(!element.contains(event.relatedTarget))update();});
  element.addEventListener('mouseleave',()=>{if(paused||reduced.matches)carousel.pause();});
  const motionChanged=()=>{paused=reduced.matches;update();};
  reduced.addEventListener('change',motionChanged);
  update();
  return ()=>{carousel.dispose();reduced.removeEventListener('change',motionChanged);};
}
