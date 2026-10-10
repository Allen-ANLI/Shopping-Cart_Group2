import { AfterViewInit, Component, ElementRef, inject, Input, OnDestroy, output, ViewChild } from '@angular/core';
import { Product } from '../models/product';
import { LanguageService } from '../services/language.service';
import { IconComponent } from '../icon/icon.component';

@Component({ selector: 'app-image-zoom', imports: [IconComponent], template: `
  <dialog #dialog class="image-dialog" (cancel)="close.emit()" (click)="backdrop($event)" [attr.aria-label]="lang.text('Enlarged product image', '商品图片放大预览')">
    <div class="image-dialog-content">
      <button type="button" class="icon-button zoom-close" autofocus (click)="close.emit()" [attr.aria-label]="lang.text('Close image', '关闭图片')"><app-icon name="close" /></button>
      <img [src]="product.imageUrl" [alt]="lang.name(product)">
      <p>{{ lang.name(product) }}</p>
    </div>
  </dialog>`,
})
export class ImageZoomComponent implements AfterViewInit, OnDestroy {
  @Input({ required: true }) product!: Product;
  @ViewChild('dialog') dialog!: ElementRef<HTMLDialogElement>;
  readonly close = output<void>();
  readonly lang = inject(LanguageService);
  private readonly opener = document.activeElement as HTMLElement | null;
  private readonly previousOverflow = document.body.style.overflow;
  ngAfterViewInit(): void {
    this.dialog.nativeElement.showModal();
    document.body.style.overflow = 'hidden';
    this.dialog.nativeElement.querySelector('button')?.focus();
  }
  backdrop(event: MouseEvent): void { if (event.target === this.dialog.nativeElement) this.close.emit(); }
  ngOnDestroy(): void {
    this.dialog.nativeElement.close();
    document.body.style.overflow = this.previousOverflow;
    this.opener?.focus();
  }
}