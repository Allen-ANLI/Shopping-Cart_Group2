import { Component, computed, input } from '@angular/core';

const iconPaths = {
  storage: ['M4 4h16v16H4Z', 'M4 14h16M7 17h.01M10 17h.01'],
  power: ['M9 2v5M15 2v5M7 7h10v5a5 5 0 0 1-10 0V7Z', 'M12 17v5'],
  wifi: ['M3 8a15 15 0 0 1 18 0M6 12a10 10 0 0 1 12 0M9 16a5 5 0 0 1 6 0M12 20h.01'],
  printer: ['M7 8V3h10v5M7 16H3V8h18v8h-4M7 13h10v8H7Z', 'M17 10h.01'],
  phone: ['M7 2h10v20H7Z', 'M10 18h4'],
  bag: ['M6 7h12l1 14H5L6 7Z', 'M9 8V6a3 3 0 0 1 6 0v2'],
  arrow: ['M5 12h14', 'm14 7 5 5-5 5'],
  heart: ['M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1.1-1.1a5.5 5.5 0 0 0-7.8 7.8L12 21l8.8-8.6a5.5 5.5 0 0 0 0-7.8Z'],
  monitor: ['M3 4h18v13H3Z', 'M8 21h8', 'M12 17v4'],
  keyboard: ['M2 6h20v12H2Z', 'M5 9h1m3 0h1m3 0h1m3 0h2M5 12h1m3 0h1m3 0h1m3 0h2M7 15h10'],
  mouse: ['M12 2a6 6 0 0 1 6 6v8a6 6 0 0 1-12 0V8a6 6 0 0 1 6-6Z', 'M12 2v7'],
  grid: ['M3 3h7v7H3ZM14 3h7v7h-7ZM3 14h7v7H3ZM14 14h7v7h-7Z'],
  check: ['m5 12 4 4L19 6'],
  search: ['M21 21l-5-5', 'M18 10a8 8 0 1 1-16 0 8 8 0 0 1 16 0Z'],
  zoom: ['M21 21l-5-5', 'M18 10a8 8 0 1 1-16 0 8 8 0 0 1 16 0Z', 'M6 10h8M10 6v8'],
  plus: ['M12 5v14M5 12h14'],
  close: ['m6 6 12 12M6 18 18 6'],
  cart: ['M2 3h3l3 12h11l3-9H6', 'M9 20h.01M18 20h.01'],
  headphones: ['M3 14v-3a9 9 0 0 1 18 0v3', 'M3 12h4v9H3ZM17 12h4v9h-4Z'],
  globe: ['M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z', 'M3 12h18M12 3c-5 5-5 13 0 18M12 3c5 5 5 13 0 18'],
} as const;

/**
 * 按图标名称提供商城页面使用的 SVG 图形。
 * @author 王重一
 */
@Component({
  selector: 'app-icon', templateUrl: './icon.component.html',
  styleUrl: './icon.component.css', host: { class: 'icon', 'aria-hidden': 'true' },
})
export class IconComponent {
  readonly name = input.required<keyof typeof iconPaths>();
  readonly paths = computed(() => iconPaths[this.name()]);
}
