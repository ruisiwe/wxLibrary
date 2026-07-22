const { request } = require('./request');

function resolveDocumentAction({ loggedIn, unlocked }) {
  if (!loggedIn) return 'LOGIN';
  return 'PREVIEW';
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
const unlock = (id, requestId) => request({ url: `/wx/documents/${id}/unlock`, method: 'POST', data: { requestId } });
const fileDisclaimer = () => request({ url: '/wx/documents/file-disclaimer' });
const original = (id, data) => request({ url: `/wx/documents/${id}/original`, method: 'POST', data });
const favorite = id => request({ url: `/wx/documents/${id}/favorite`, method: 'POST' });
const unfavorite = id => request({ url: `/wx/documents/${id}/favorite`, method: 'DELETE' });
const unlocked = () => request({ url: '/wx/documents/unlocked' });
const favorites = () => request({ url: '/wx/documents/favorites' });

module.exports = { resolveDocumentAction, canPreview, buildShareOptions, home, categories, list, detail, preview, unlock, fileDisclaimer, original, favorite, unfavorite, unlocked, favorites };
