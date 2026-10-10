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
  effectivePrice?: number;
  discountPercent?: number;
  active: boolean;
  category?: string;
  brand?: string;
  origin?: string;
  nameZh?: string;
  descriptionZh?: string;
  originZh?: string;
  averageRating?: number;
  totalReviews?: number;
  salesCount?: number;
  stockQuantity?: number;
}
export interface ProductReview { id: number; displayName: string; rating: number; comment: string; createdAt: string; sample?: boolean; }
export interface ReviewSummary { reviews: ProductReview[]; averageRating: number; totalReviews: number; canReview: boolean; loggedIn?: boolean; ownReview?: {rating: number; comment: string} | null; }
export const CATEGORIES = [
  { slug: 'computing', name: 'Computing', nameZh: '电脑与配件' },
  { slug: 'typing', name: 'Keyboards & mice', nameZh: '键盘与鼠标' },
  { slug: 'workspace', name: 'Workspace', nameZh: '桌面好物' },
  { slug: 'audio', name: 'Audio', nameZh: '音频设备' },
  { slug: 'displays', name: 'Monitors & displays', nameZh: '显示器与屏幕' },
  { slug: 'storage', name: 'Storage', nameZh: '存储设备' },
  { slug: 'charging', name: 'Charging & power', nameZh: '充电与电源' },
  { slug: 'networking', name: 'Networking', nameZh: '网络设备' },
  { slug: 'printing', name: 'Printing & scanning', nameZh: '打印与扫描' },
  { slug: 'mobile', name: 'Mobile & tablets', nameZh: '手机与平板' },
] as const;
export interface ProductPage {
  content: Product[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
