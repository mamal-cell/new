const { app, BrowserWindow, dialog, ipcMain, shell } = require('electron');
const path = require('node:path');
const fs = require('node:fs');

function createWindow() {
  const win = new BrowserWindow({
    width: 1440, height: 960, minWidth: 1050, minHeight: 720,
    backgroundColor: '#f5f8f6', title: 'Electruck',
    webPreferences: { preload: path.join(__dirname, 'preload.cjs'), contextIsolation: true, nodeIntegration: false, sandbox: true }
  });
  win.loadFile('index.html');
}

app.whenReady().then(() => {
  createWindow();
  app.on('activate', () => { if (BrowserWindow.getAllWindows().length === 0) createWindow(); });
});
app.on('window-all-closed', () => { if (process.platform !== 'darwin') app.quit(); });

ipcMain.handle('file:open', async () => {
  const result = await dialog.showOpenDialog({ properties: ['openFile'], filters: [
    { name: 'نقشه و تصویر', extensions: ['png', 'jpg', 'jpeg', 'webp', 'pdf'] }, { name: 'همهٔ فایل‌ها', extensions: ['*'] }
  ] });
  if (result.canceled || !result.filePaths[0]) return null;
  const filePath = result.filePaths[0];
  const ext = path.extname(filePath).toLowerCase();
  let preview = null;
  if (['.png', '.jpg', '.jpeg', '.webp'].includes(ext)) {
    const data = fs.readFileSync(filePath);
    preview = `data:${ext === '.jpg' ? 'image/jpeg' : `image/${ext.slice(1)}`};base64,${data.toString('base64')}`;
  }
  return { name: path.basename(filePath), path: filePath, preview, type: ext };
});
ipcMain.handle('file:save', async (_event, { name, content }) => {
  const result = await dialog.showSaveDialog({ defaultPath: name, filters: [{ name: 'Electruck project', extensions: ['json'] }] });
  if (result.canceled || !result.filePath) return false;
  fs.writeFileSync(result.filePath, content, 'utf8');
  return true;
});
ipcMain.handle('file:open-project', async () => {
  const result = await dialog.showOpenDialog({ properties: ['openFile'], filters: [{ name: 'Electruck project', extensions: ['json'] }] });
  if (result.canceled || !result.filePaths[0]) return null;
  try { return JSON.parse(fs.readFileSync(result.filePaths[0], 'utf8')); }
  catch { return { error: 'فایل پروژه معتبر نیست.' }; }
});
ipcMain.handle('external:open', (_event, url) => {
  if (typeof url === 'string' && url.startsWith('https://')) shell.openExternal(url);
});
