export function isAdmin() {
  return sessionStorage.getItem('adminVerified') === 'true'
}