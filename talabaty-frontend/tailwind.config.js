export default {
    darkMode: 'class',
    content: [
        "./index.html",
        "./src/**/*.{js,ts,jsx,tsx}",
    ],
    theme: {
        extend: {
            fontFamily: {
                sans: ['Sora', '-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'sans-serif'],
                cairo: ['Cairo', 'Sora', '-apple-system', 'BlinkMacSystemFont', 'sans-serif'],
            },
            colors: {
                primary: {
                    50: '#f0f9ff',
                    100: '#e0f2fe',
                    200: '#bae6fd',
                    300: '#7dd3fc',
                    400: '#38bdf8',
                    500: '#0ea5e9',
                    600: '#0284c7',
                    700: '#0369a1',
                    800: '#075985',
                    900: '#0c4a6e',
                },
                saas: {
                    bg: '#F6F8FB',
                    card: '#FFFFFF',
                    border: '#E6E8EC',
                    'text-primary': '#111827',
                    'text-secondary': '#6B7280',
                },
            },
        },
    },
    plugins: [],
};
