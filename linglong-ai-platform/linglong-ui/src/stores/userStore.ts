import { create } from 'zustand'
import { persist } from 'zustand/middleware'

export interface User {
  id: number
  username: string
  mobile?: string
  role: number // 0=普通用户, 1=系统管理员
}

interface UserState {
  user: User | null
  token: string | null
  setUser: (user: User) => void
  setToken: (token: string) => void
  logout: () => void
  isAuthenticated: () => boolean
  isAdmin: () => boolean
}

export const useUserStore = create<UserState>()(
  persist(
    (set, get) => ({
      user: null,
      token: null,
      setUser: (user) => set({ user }),
      setToken: (token) => {
        localStorage.setItem('token', token)
        set({ token })
      },
      logout: () => {
        localStorage.removeItem('token')
        localStorage.removeItem('user-storage')
        set({ user: null, token: null })
      },
      isAuthenticated: () => {
        const state = get()
        return !!state.token && !!state.user
      },
      isAdmin: () => {
        const state = get()
        return state.user?.role === 1
      },
    }),
    {
      name: 'user-storage',
      partialize: (state) => ({ user: state.user, token: state.token }),
    }
  )
)
