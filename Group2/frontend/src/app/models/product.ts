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
export interface Category { slug: string; name: string; nameZh: string; count: number; }
export interface ProductReview { id: number; displayName: string; rating: number; comment: string; createdAt: string; sample?: boolean; }
export interface ReviewSummary { reviews: ProductReview[]; averageRating: number; totalReviews: number; canReview: boolean; loggedIn?: boolean; ownReview?: {rating: number; comment: string} | null; }
export const CATEGORIES = [
  { slug: 'computing', name: 'Computing', nameZh: '电脑与配件', icon: 'monitor', description: 'Connect to a better workday.', descriptionZh: '高效连接，轻松工作。' },
  { slug: 'typing', name: 'Keyboards & mice', nameZh: '键盘与鼠标', icon: 'keyboard', description: 'Make every keystroke count.', descriptionZh: '每次输入都舒适自如。' },
  { slug: 'workspace', name: 'Workspace', nameZh: '桌面好物', icon: 'mouse', description: 'A little room for inspiration.', descriptionZh: '为桌面增添更多灵感。' },
  { slug: 'audio', name: 'Audio', nameZh: '音频设备', icon: 'headphones', description: 'Find your everyday soundtrack.', descriptionZh: '听见日常的好声音。' },
  { slug: 'displays', name: 'Monitors & displays', nameZh: '显示器与屏幕', icon: 'monitor', description: 'More room to create.', descriptionZh: '更多视野，更多创造。' },
  { slug: 'storage', name: 'Storage', nameZh: '存储设备', icon: 'storage', description: 'Keep every idea close.', descriptionZh: '灵感与资料，随时保存。' },
  { slug: 'charging', name: 'Charging & power', nameZh: '充电与电源', icon: 'power', description: 'Power through your day.', descriptionZh: '全天续航，工作不断电。' },
  { slug: 'networking', name: 'Networking', nameZh: '网络设备', icon: 'wifi', description: 'A stronger connection.', descriptionZh: '稳定连接，高效协作。' },
  { slug: 'printing', name: 'Printing & scanning', nameZh: '打印与扫描', icon: 'printer', description: 'Bring ideas to paper.', descriptionZh: '让每个想法跃然纸上。' },
  { slug: 'mobile', name: 'Mobile & tablets', nameZh: '手机与平板', icon: 'phone', description: 'Work beyond your desk.', descriptionZh: '移动办公，随处从容。' },
] as const;
export interface ProductPage {
  content: Product[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
