# Talabaty Frontend

A modern React + TypeScript frontend for the Talabaty order management system.

## Features

- 🔐 Authentication (Login/Signup)
- 📊 Dashboard with statistics
- 🏪 Store management
- 📦 Order management
- 🚚 Shipping provider configuration (Ozon Express)
- 📱 Responsive design with Tailwind CSS

## Tech Stack

- **React 18** - UI library
- **TypeScript** - Type safety
- **React Router** - Routing
- **Axios** - HTTP client
- **Tailwind CSS** - Styling
- **Vite** - Build tool
- **Lucide React** - Icons

## Getting Started

### Prerequisites

- Node.js 18+ and npm

### Installation

1. Install dependencies:
```bash
npm install
```

2. Start the development server:
```bash
npm run dev
```

The app will be available at `http://localhost:3000`

### Configuration

The frontend is configured to proxy API requests to `http://localhost:8080` by default. You can change this in `vite.config.ts` or set the `VITE_API_URL` environment variable.

## Project Structure

```
src/
├── components/          # Reusable components
│   ├── Layout.tsx      # Main layout with sidebar
│   ├── CreateStoreModal.tsx
│   ├── CreateOrderModal.tsx
│   ├── ShippingProviderForm.tsx
│   └── SendToShippingModal.tsx
├── contexts/            # React contexts
│   └── AuthContext.tsx # Authentication context
├── pages/              # Page components
│   ├── Login.tsx
│   ├── Signup.tsx
│   ├── Dashboard.tsx
│   ├── Stores.tsx
│   ├── StoreDetail.tsx
│   ├── Orders.tsx
│   ├── OrderDetail.tsx
│   └── ShippingProviders.tsx
└── services/           # API services
    ├── api.ts         # Axios instance
    ├── authService.ts
    ├── storeService.ts
    ├── orderService.ts
    └── shippingService.ts
```

## Available Scripts

- `npm run dev` - Start development server
- `npm run build` - Build for production
- `npm run preview` - Preview production build

## API Integration

The frontend communicates with the backend API at `/api`. All requests are automatically authenticated using JWT tokens stored in localStorage.

### Authentication Flow

1. User logs in or signs up
2. Access token and refresh token are stored in localStorage
3. All API requests include the access token in the Authorization header
4. On 401 errors, user is redirected to login

## Features Overview

### Dashboard
- Overview statistics (stores, orders, pending orders, shipped orders)
- Recent orders list

### Stores
- List all stores
- Create new stores
- View store details
- Configure shipping providers per store

### Orders
- List orders with filtering by store and status
- Create new orders
- View order details
- Update order status
- Send orders to shipping (Ozon Express)

### Shipping Providers
- Configure Ozon Express per store
- View all shipping provider configurations

## Environment Variables

Create a `.env` file in the root directory:

```
VITE_API_URL=http://localhost:8080/api
```

## Building for Production

```bash
npm run build
```

The built files will be in the `dist/` directory.

## License

MIT

