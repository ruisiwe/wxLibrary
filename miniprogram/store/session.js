const TOKEN_KEY = 'wx-library-token';
const USER_KEY = 'wx-library-user';

let memory = { token: '', user: null };

function storage() {
  return typeof wx === 'undefined' ? null : wx;
}

function restore() {
  const api = storage();
  if (api) {
    memory = {
      token: api.getStorageSync(TOKEN_KEY) || '',
      user: api.getStorageSync(USER_KEY) || null
    };
  }
  return { ...memory };
}

function save(token, user) {
  memory = { token: token || '', user: user || null };
  const api = storage();
  if (api) {
    api.setStorageSync(TOKEN_KEY, memory.token);
    api.setStorageSync(USER_KEY, memory.user);
  }
  return { ...memory };
}

function clear() {
  return save('', null);
}

function getToken() {
  return memory.token || restore().token;
}

function getUser() {
  return memory.user || restore().user;
}

module.exports = { restore, save, clear, getToken, getUser };
