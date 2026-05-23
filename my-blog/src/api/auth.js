const API_BASE_URL = import.meta.env.VITE_API_URL || '/api'

class AuthAPI {
  constructor() {
    this.baseURL = API_BASE_URL
  }

  async verify(username, password) {
    const response = await fetch(`${this.baseURL}/verify`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ username, password }),
      credentials: 'include'
    })
    return await response.json()
  }
}

export default new AuthAPI()