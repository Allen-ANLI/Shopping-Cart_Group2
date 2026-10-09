/**
 * 备份并部署商品前端静态资源，同步项目中已有的后端编译输出。
 * @author 王重一
 * @author 李岸 Li An (F portable deployment backup)
 */
import { cp, lstat, mkdir, readFile, realpath, rm } from 'node:fs/promises';
import path from 'node:path';

// Only this project's product static directories may be replaced.
const frontend = path.resolve(import.meta.dirname, '..');
const project = await realpath(path.resolve(frontend, '..'));
const build = path.join(frontend, 'dist', 'browser');
const html = await readFile(path.join(build, 'index.html'), 'utf8');
if (!html.includes('<app-root>') || !html.includes('<base href="/products/">')) {
  throw new Error('Build the Angular production application before deploying.');
}
const destinations = [path.join(project, 'src/main/resources/static/products')];
try {
  await lstat(path.join(project, 'target/classes/static'));
  destinations.push(path.join(project, 'target/classes/static/products'));
} catch (error) {
  if (error.code !== 'ENOENT') throw error;
}
for (const destination of destinations) {
  const parent = await realpath(path.dirname(destination));
  if (!parent.startsWith(project + path.sep) || path.basename(destination) !== 'products') {
    throw new Error('Deployment path must stay inside this project.');
  }
  try {
    const stat = await lstat(destination);
    if (stat.isSymbolicLink() || !(await realpath(destination)).startsWith(project + path.sep)) {
      throw new Error('Refusing to replace a linked or external directory.');
    }
  } catch (error) {
    if (error.code !== 'ENOENT') throw error;
  }
}
const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
// F integration: use a portable project-local backup unless explicitly overridden.
const backup = path.join(process.env['SHOPPING_CART_BACKUP_DIR'] ?? path.join(project, '.deploy-backups'),
  `shopping-cart-deploy-${timestamp}`);
await mkdir(backup, { recursive: true });
for (const [index, destination] of destinations.entries()) {
  try { await cp(destination, path.join(backup, index === 0 ? 'source' : 'target'), { recursive: true }); }
  catch (error) { if (error.code !== 'ENOENT') throw error; }
}
for (const destination of destinations) {
  await rm(destination, { recursive: true, force: true });
  await mkdir(destination, { recursive: true });
  await cp(build, destination, { recursive: true });
  console.log(`Deployed Angular: ${destination}`);
}
console.log(`Recoverable previous deployment: ${backup}`);
