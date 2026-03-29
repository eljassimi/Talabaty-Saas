import { createContext, useContext, useEffect, useState, ReactNode } from 'react';
const STORAGE_KEY = 'talabaty-theme';
type Theme = 'light' | 'dark';
interface ThemeContextType {
    theme: Theme;
    toggleTheme: () => void;
}
const ThemeContext = createContext<ThemeContextType | undefined>(undefined);
function getInitialTheme(): Theme {
    if (typeof window === 'undefined')
        return 'light';
    try {
        const stored = localStorage.getItem(STORAGE_KEY) as Theme | null;
        if (stored === 'dark' || stored === 'light')
            return stored;
    }
    catch (_) { }
    return 'light';
}
export function ThemeProvider({ children }: {
    children: ReactNode;
}) {
    const [theme, setTheme] = useState<Theme>(getInitialTheme);
    useEffect(() => {
        const root = document.documentElement;
        root.classList.remove('light', 'dark');
        root.classList.add(theme);
        try {
            localStorage.setItem(STORAGE_KEY, theme);
        }
        catch (_) { }
    }, [theme]);
    const toggleTheme = () => {
        setTheme((prev) => (prev === 'light' ? 'dark' : 'light'));
    };
    return (<ThemeContext.Provider value={{ theme, toggleTheme }}>
      {children}
    </ThemeContext.Provider>);
}
export function useTheme() {
    const context = useContext(ThemeContext);
    if (context === undefined) {
        throw new Error('useTheme must be used within a ThemeProvider');
    }
    return context;
}
