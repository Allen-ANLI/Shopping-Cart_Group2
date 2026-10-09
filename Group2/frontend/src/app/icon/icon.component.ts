import { Component, computed, input } from '@angular/core';

const iconPaths = {
  bag: ['M6 7h12l1 14H5L6 7Z', 'M9 8V6a3 3 0 0 1 6 0v2'],
  arrow: ['M5 12h14', 'm14 7 5 5-5 5'],
  heart: ['M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1.1-1.1a5.5 5.5 0 0 0-7.8 7.8L12 21l8.8-8.6a5.5 5.5 0 0 0 0-7.8Z'],
  monitor: ['M3 4h18v13H3Z', 'M8 21h8', 'M12 17v4'],
  keyboard: ['M2 6h20v12H2Z', 'M5 9h1m3 0h1m3 0h1m3 0h2M5 12h1m3 0h1m3 0h1m3 0h2M7 15h10'],
  mouse: ['M12 2a6 6 0 0 1 6 6v8a6 6 0 0 1-12 0V8a6 6 0 0 1 6-6Z', 'M12 2v7'],
  grid: ['M3 3h7v7H3ZM14 3h7v7h-7ZM3 14h7v7H3ZM14 14h7v7h-7Z'],
  check: ['m5 12 4 4L19 6'],
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