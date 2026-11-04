// Iconos vectoriales del campus: un mismo trazo y sin dependencias ni imágenes externas.
const paths = {
  book: '<path d="M4 4h6a3 3 0 0 1 3 3v14a4 4 0 0 0-4-3H4z"/><path d="M20 4h-4a3 3 0 0 0-3 3v14a4 4 0 0 1 4-3h3z"/>',
  chat: '<path d="M21 11a9 9 0 0 1-9 9H4l-3 2 2-6a9 9 0 1 1 18-5Z"/><path d="M7 9h9M7 13h6"/>',
  chart: '<path d="M4 3v18h17M8 17v-5M13 17V7M18 17V4"/>',
  cap: '<path d="m2 9 10-5 10 5-10 5zM6 11v6c4 3 8 3 12 0v-6M22 9v8"/>',
  arrow: '<path d="M5 12h14m-6-6 6 6-6 6"/>',
  calendar: '<rect x="3" y="5" width="18" height="16" rx="2"/><path d="M7 3v4m10-4v4M3 11h18M7 15h3m4 0h3"/>',
  user: '<circle cx="12" cy="8" r="4"/><path d="M4 22v-2a8 8 0 0 1 16 0v2"/>',
  bell: '<path d="M6 9a6 6 0 0 1 12 0v5l3 3H3l3-3zM9 21h6"/>',
};
export const icon = name => `<svg class="ui-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${paths[name] || paths.book}</svg>`;
