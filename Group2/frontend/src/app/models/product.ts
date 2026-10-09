/**
 * 定义商品及分页响应的 TypeScript 类型，与后端商品 JSON 字段对应。
 * @author 王重一
 */
export interface Product {
  id: number;
  name: string;
  description: string | null;
  imageUrl: string | null;
  price: number;
  active: boolean;
}
export interface ProductPage {
  content: Product[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}