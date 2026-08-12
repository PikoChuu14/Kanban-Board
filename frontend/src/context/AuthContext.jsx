import { createContext, useContext, useState } from "react";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(localStorage.getItem("token"));

  const [user, setUser] = useState(() => {
    const savedUser = localStorage.getItem("user");

    return savedUser ? JSON.parse(savedUser) : null;
  });

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
    localStorage.setItem("user", JSON.stringify(loggedInUser));

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
        isAuthenticated: Boolean(token),
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
