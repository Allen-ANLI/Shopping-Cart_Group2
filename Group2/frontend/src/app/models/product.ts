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
  category?: string;
  brand?: string;
  origin?: string;
  nameZh?: string;
  descriptionZh?: string;
  originZh?: string;
}
export interface Category { slug: string; name: string; nameZh: string; count: number; }
export interface ProductReview { id: number; displayName: string; rating: number; comment: string; createdAt: string; }
export interface ReviewSummary { reviews: ProductReview[]; averageRating: number; totalReviews: number; canReview: boolean; ownReview?: {rating: number; comment: string} | null; }
export const CATEGORIES = [
  { slug: 'computing', name: 'Computing', nameZh: '电脑配件', icon: 'monitor', description: 'Connect to a better workday.', descriptionZh: '高效连接，轻松工作。' },
  { slug: 'typing', name: 'Typing', nameZh: '键盘与输入', icon: 'keyboard', description: 'Make every keystroke count.', descriptionZh: '每次输入都舒适自如。' },
  { slug: 'workspace', name: 'Workspace', nameZh: '桌面好物', icon: 'mouse', description: 'A little room for inspiration.', descriptionZh: '为桌面增添更多灵感。' },
  { slug: 'audio', name: 'Audio', nameZh: '音频设备', icon: 'headphones', description: 'Find your everyday soundtrack.', descriptionZh: '听见日常的好声音。' },
] as const;
export interface ProductPage {
  content: Product[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
