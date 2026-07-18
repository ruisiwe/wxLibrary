const { request } = require('./request');

function resolveDocumentAction({ loggedIn, unlocked }) {
  if (!loggedIn) return 'LOGIN';
  return unlocked ? 'FULL' : 'PREVIEW';
}

function canPreview({ loggedIn }) {
  return Boolean(loggedIn);
}

function buildShareOptions(filePath) {
  return { filePath };
}

const home = () => request({ url: '/wx/public/home', protected: false });
const categories = () => request({ url: '/wx/public/categories', protected: false });
const list = params => request({ url: '/wx/public/documents', data: params, protected: false });
const detail = id => request({ url: `/wx/public/documents/${id}`, protected: false });
const preview = id => request({ url: `/wx/documents/${id}/preview` });
const full = id => request({ url: `/wx/documents/${id}/full` });
const unlock = (id, requestId) => request({ url: `/wx/documents/${id}/unlock`, method: 'POST', data: { requestId } });
const original = id => request({ url: `/wx/documents/${id}/original`, method: 'POST' });
const favorite = id => request({ url: `/wx/documents/${id}/favorite`, method: 'POST' });
const unfavorite = id => request({ url: `/wx/documents/${id}/favorite`, method: 'DELETE' });
const unlocked = () => request({ url: '/wx/documents/unlocked' });
const favorites = () => request({ url: '/wx/documents/favorites' });

module.exports = { resolveDocumentAction, canPreview, buildShareOptions, home, categories, list, detail, preview, full, unlock, original, favorite, unfavorite, unlocked, favorites };
