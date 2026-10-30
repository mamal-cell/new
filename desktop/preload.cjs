const { contextBridge, ipcRenderer } = require('electron');
contextBridge.exposeInMainWorld('electruck', {
  openFile: () => ipcRenderer.invoke('file:open'),
  saveProject: (payload) => ipcRenderer.invoke('file:save', payload),
  openProject: () => ipcRenderer.invoke('file:open-project'),
  openExternal: (url) => ipcRenderer.invoke('external:open', url)
});
