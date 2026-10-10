import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ImageZoomComponent } from './image-zoom.component';
import { keyboard } from '../testing/product-fixtures';

describe('Product image zoom accessibility', () => {
  let fixture: ComponentFixture<ImageZoomComponent>;
  let opener: HTMLButtonElement;
  beforeEach(async () => {
    document.cookie = 'store_lang=en; Path=/';
    Object.defineProperty(HTMLDialogElement.prototype, 'showModal', { configurable: true, value: vi.fn(function(this: HTMLDialogElement) { this.open = true; }) });
    Object.defineProperty(HTMLDialogElement.prototype, 'close', { configurable: true, value: vi.fn(function(this: HTMLDialogElement) { this.open = false; }) });
    await TestBed.configureTestingModule({ imports: [ImageZoomComponent] }).compileComponents();
    opener = document.createElement('button'); document.body.appendChild(opener); opener.focus();
    fixture = TestBed.createComponent(ImageZoomComponent); fixture.componentRef.setInput('product', keyboard); fixture.detectChanges();
  });
  afterEach(() => { fixture.destroy(); opener.remove(); });
  it('opens a native modal, gives it a label and focuses its close button', () => {
    const dialog = fixture.nativeElement.querySelector('dialog') as HTMLDialogElement;
    expect(dialog.open).toBe(true);
    expect(dialog.getAttribute('aria-label')).toBe('Enlarged product image');
    expect(document.activeElement).toBe(dialog.querySelector('button'));
    expect(document.body.style.overflow).toBe('hidden');
    expect(dialog.querySelector('img')?.getAttribute('src')).toBe(keyboard.imageUrl);
  });
  it('closes on Escape, its close button or backdrop while clicks inside leave it open', () => {
    const close = vi.fn(); fixture.componentInstance.close.subscribe(close);
    const dialog = fixture.nativeElement.querySelector('dialog') as HTMLDialogElement;
    dialog.querySelector('img')!.dispatchEvent(new MouseEvent('click', { bubbles: true })); expect(close).not.toHaveBeenCalled();
    dialog.dispatchEvent(new Event('cancel')); expect(close).toHaveBeenCalledTimes(1);
    dialog.querySelector('button')!.click(); expect(close).toHaveBeenCalledTimes(2);
    dialog.dispatchEvent(new MouseEvent('click')); expect(close).toHaveBeenCalledTimes(3);
  });
  it('restores the original focused image button and page scrolling on close', () => {
    fixture.destroy(); expect(document.activeElement).toBe(opener); expect(document.body.style.overflow).toBe('');
  });
});