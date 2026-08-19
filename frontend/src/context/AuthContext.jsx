import {
  createContext,
  useContext,
  useEffect,
  useState,
} from "react";

import { apiFetch } from "../api/apiFetch";

const AuthContext = createContext(null);
const API_BASE_URL = "";

export function AuthProvider({ children }) {
  const [token, setToken] = useState(localStorage.getItem("token"));
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function restoreSession() {
      if (!token) {
        setLoading(false);
        return;
      }

      try {
        const response = await apiFetch(`${API_BASE_URL}/api/auth/me`);

        if (!response.ok) {
          throw new Error("Invalid session");
        }

        setUser(await response.json());
      } catch (error) {
        console.error("Failed to restore session:", error);
        localStorage.removeItem("token");
        localStorage.removeItem("user");
        setToken(null);
        setUser(null);
      } finally {
        setLoading(false);
      }
    }

    restoreSession();
  }, [token]);

  function login(loginResponse) {
    const loggedInUser = {
      userId: loginResponse.userId,
      name: loginResponse.name,
      email: loginResponse.email,
      role: loginResponse.role,
      departmentId: loginResponse.departmentId,
      departmentName: loginResponse.departmentName,
    };

    localStorage.setItem("token", loginResponse.token);
    localStorage.removeItem("user");

    setLoading(true);
    setToken(loginResponse.token);
    setUser(loggedInUser);
  }

  function logout() {
    localStorage.removeItem("token");
    localStorage.removeItem("user");

    setToken(null);
    setUser(null);
  }

  return (
    <AuthContext.Provider
      value={{
        token,
        user,
        login,
        logout,
        loading,
        isAuthenticated: Boolean(token && user),
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
