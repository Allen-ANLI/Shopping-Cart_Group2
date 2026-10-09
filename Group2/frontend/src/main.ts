/**
 * 启动 Angular 商品前端并加载根组件与应用配置。
 * @author 王重一
 */
import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';
bootstrapApplication(AppComponent, appConfig).catch((error: unknown) => console.error(error));