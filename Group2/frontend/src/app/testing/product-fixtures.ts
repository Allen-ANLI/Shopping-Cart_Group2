/**
 * 提供商品前端测试使用的模拟商品和分页响应数据。
 * @author 王重一
 */
import { Product, ProductPage } from '../models/product';
export const keyboard: Product = {
  id: 1, name: 'Keyboard', description: 'A comfortable keyboard for everyday use.',
  imageUrl: '/images/keyboard.png', price: 50, active: true,
};
export const mouse: Product = {
  id: 2, name: 'Mouse', description: 'A wireless mouse for work and study.',
  imageUrl: '/images/mouse.png', price: 20, active: true,
};
export function productPage(content: Product[], page = 0, size = 6, totalElements = 2): ProductPage {
  return { content, page, size, totalElements, totalPages: Math.ceil(totalElements / size) };
}