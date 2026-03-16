# Talabaty - Documentation Technique du Projet

## Projet de Fin d'Études

---

## Table des Matières

1. [Introduction et Contexte](#1-introduction-et-contexte)
2. [Objectifs du Projet](#2-objectifs-du-projet)
3. [Architecture Générale](#3-architecture-générale)
4. [Stack Technique](#4-stack-technique)
5. [Architecture Backend (Spring Boot)](#5-architecture-backend-spring-boot)
6. [Architecture Frontend (React)](#6-architecture-frontend-react)
7. [Base de Données](#7-base-de-données)
8. [Sécurité et Authentification](#8-sécurité-et-authentification)
9. [Fonctionnalités Détaillées](#9-fonctionnalités-détaillées)
10. [Intégrations Externes](#10-intégrations-externes)
11. [Microservice WhatsApp Bridge](#11-microservice-whatsapp-bridge)
12. [API REST Complète](#12-api-rest-complète)
13. [Déploiement et DevOps](#13-déploiement-et-devops)
14. [Design Patterns et Bonnes Pratiques](#14-design-patterns-et-bonnes-pratiques)
15. [Diagrammes](#15-diagrammes)
16. [Conclusion](#16-conclusion)

---

## 1. Introduction et Contexte

### 1.1 Problématique

Au Maroc, les e-commerçants et les boutiques en ligne font face à plusieurs défis majeurs dans la gestion de leurs commandes :

- **Dispersion des sources de commandes** : les commandes arrivent de multiples canaux (plateformes e-commerce, fichiers Excel, Google Sheets, appels téléphoniques, WhatsApp).
- **Suivi de livraison fragmenté** : les transporteurs offrent leurs propres interfaces, obligeant les commerçants à jongler entre plusieurs plateformes.
- **Communication client manuelle** : informer chaque client de l'état de sa commande par WhatsApp est chronophage.
- **Gestion d'équipe complexe** : les équipes de support (confirmation téléphonique) et de management travaillent sans outil centralisé.
- **Absence de visibilité** : pas de dashboard unifié pour suivre les performances commerciales.

### 1.2 Solution Proposée : Talabaty

**Talabaty** (طلباتي - "Mes Commandes" en arabe marocain) est une plateforme SaaS multi-tenant de gestion de commandes conçue pour le marché marocain. Elle offre :

- Un **système centralisé** pour agréger les commandes de toutes les sources
- Une **intégration directe** avec les transporteurs (Ozon Express)
- Une **automatisation WhatsApp** pour la communication client
- Un **système de gestion d'équipe** avec rôles et permissions
- Un **dashboard analytique** en temps réel
- Un **système de rémunération** pour les équipes de support

### 1.3 Public Cible

- E-commerçants marocains (boutiques en ligne)
- Vendeurs sur les réseaux sociaux (Instagram, Facebook)
- Entreprises de vente par téléphone (call centers)
- Revendeurs utilisant des plateformes comme YouCan

---

## 2. Objectifs du Projet

### 2.1 Objectifs Fonctionnels

| # | Objectif | Description |
|---|----------|-------------|
| OF1 | Gestion multi-source des commandes | Créer, importer (Excel, Google Sheets) et synchroniser (YouCan) les commandes |
| OF2 | Suivi de livraison intégré | Envoyer les commandes au transporteur et suivre en temps réel |
| OF3 | Automatisation WhatsApp | Envoyer automatiquement des messages aux clients lors des changements de statut |
| OF4 | Gestion multi-boutique | Permettre à un compte de gérer plusieurs boutiques indépendantes |
| OF5 | Gestion d'équipe avec rôles | Système de rôles (Admin, Owner, Manager, Support) avec permissions granulaires |
| OF6 | Dashboard analytique | Visualisation des KPIs : commandes, revenus, taux de succès, répartition géographique |
| OF7 | Système de rémunération support | Calcul automatique des gains par commande confirmée/livrée |
| OF8 | API publique | Permettre l'intégration avec des systèmes tiers via API key |

### 2.2 Objectifs Techniques

| # | Objectif | Description |
|---|----------|-------------|
| OT1 | Architecture en couches | Séparation claire : API, Domaine, Infrastructure |
| OT2 | Sécurité robuste | Authentification JWT, RBAC, chiffrement BCrypt |
| OT3 | Multi-tenancy | Isolation des données par compte (Account) |
| OT4 | Containerisation | Déploiement via Docker Compose |
| OT5 | Migration de schéma | Gestion des évolutions de BDD avec Liquibase |
| OT6 | Architecture microservices | WhatsApp Bridge comme service indépendant |
| OT7 | UI/UX moderne | Interface responsive avec thème dynamique par boutique |

---

## 3. Architecture Générale

### 3.1 Vue d'Ensemble

```
┌─────────────────────────────────────────────────────────────────────┐
│                        NAVIGATEUR CLIENT                            │
│                    (React + TypeScript + Vite)                       │
└─────────────┬───────────────────────────────────┬───────────────────┘
              │ HTTPS (REST API)                  │ WebGL (Three.js)
              ▼                                   │ (Login page only)
┌─────────────────────────────────┐               │
│      NGINX (Reverse Proxy)      │               │
│         Port 80/443             │               │
└─────────────┬───────────────────┘               │
              │                                   │
              ▼                                   │
┌─────────────────────────────────┐               │
│   SPRING BOOT APPLICATION       │               │
│        Port 8080                │               │
│                                 │               │
│  ┌───────────────────────────┐  │               │
│  │   API Layer (Controllers) │  │               │
│  │   + DTOs + Mappers        │  │               │
│  ├───────────────────────────┤  │               │
│  │  Security Layer (JWT/RBAC)│  │               │
│  ├───────────────────────────┤  │               │
│  │   Domain Layer (Services) │  │               │
│  ├───────────────────────────┤  │               │
│  │ Infrastructure (JPA/Repos)│  │               │
│  └───────────────────────────┘  │               │
└──────┬──────────┬───────────────┘               │
       │          │                               │
       ▼          ▼                               │
┌──────────┐ ┌──────────────────┐                 │
│PostgreSQL│ │ WhatsApp Bridge  │                 │
│Port 5432 │ │   (Node.js)     │                 │
│          │ │   Port 3100     │                 │
└──────────┘ └───────┬──────────┘                 │
                     │                            │
                     ▼                            │
              ┌──────────────┐                    │
              │  WhatsApp    │                    │
              │  (Web API)   │                    │
              └──────────────┘                    │
```

### 3.2 Architecture Microservices

Le projet est composé de **3 services** :

| Service | Technologie | Port | Rôle |
|---------|-------------|------|------|
| **app** (Backend + Frontend) | Java 17 / Spring Boot 4.0 | 8080 | API REST, logique métier, serveur de fichiers statiques |
| **whatsapp-bridge** | Node.js 18 / Express | 3100 | Passerelle WhatsApp gratuite via whatsapp-web.js |
| **postgres** | PostgreSQL 16 | 5432 | Base de données relationnelle |

### 3.3 Pattern Architectural : DDD-Lite (Domain-Driven Design Léger)

```
src/main/java/ma/talabaty/talabaty/
├── api/                          # Couche Présentation
│   ├── controllers/              #   Contrôleurs REST (15 controllers)
│   ├── dtos/                     #   Objets de transfert de données
│   └── mappers/                  #   Mappers Entity <-> DTO
├── core/                         # Couche Transversale
│   ├── exceptions/               #   Gestion globale des erreurs
│   ├── scheduling/               #   Tâches planifiées (cron)
│   ├── security/                 #   JWT, RBAC, filtres d'auth
│   └── storage/                  #   Stockage de fichiers
└── domain/                       # Couche Domaine (Métier)
    ├── accounts/                 #   Gestion des comptes
    ├── orders/                   #   Gestion des commandes
    │   ├── model/                #     Entités JPA
    │   ├── repository/           #     Repositories Spring Data
    │   └── service/              #     Services métier
    ├── shipping/                 #   Intégration transporteurs
    ├── stores/                   #   Gestion des boutiques
    ├── support/                  #   Revenus équipe support
    ├── teams/                    #   Gestion des équipes
    ├── whatsapp/                 #   Service WhatsApp
    └── youcan/                   #   Intégration YouCan
```

---

## 4. Stack Technique

### 4.1 Backend

| Technologie | Version | Utilisation |
|-------------|---------|-------------|
| **Java** | 17 (LTS) | Langage de programmation principal |
| **Spring Boot** | 4.0.0 | Framework applicatif |
| **Spring Security** | 6.x | Authentification et autorisation |
| **Spring Data JPA** | (managed) | Couche d'accès aux données (ORM Hibernate) |
| **Liquibase** | (managed) | Migration et versionnement du schéma BDD |
| **PostgreSQL** | 16 | Base de données relationnelle |
| **JJWT** | 0.12.3 | Génération et validation de tokens JWT |
| **Apache POI** | 5.2.5 | Lecture/écriture de fichiers Excel (.xlsx) |
| **Google API Client** | 2.2.0 | Intégration Google Sheets |
| **Lombok** | (managed) | Réduction du code boilerplate |
| **Jackson** | (managed) | Sérialisation/désérialisation JSON |
| **Maven** | 3.x | Gestionnaire de build et dépendances |

### 4.2 Frontend

| Technologie | Version | Utilisation |
|-------------|---------|-------------|
| **React** | 18.3.1 | Bibliothèque UI (SPA) |
| **TypeScript** | 5.9.3 | Typage statique JavaScript |
| **Vite** | 7.2.4 | Outil de build et serveur de développement |
| **Tailwind CSS** | 3.4.13 | Framework CSS utility-first |
| **React Router** | 6.26.0 | Routage côté client |
| **Axios** | 1.7.7 | Client HTTP pour les appels API |
| **Chart.js** | 4.5.1 | Bibliothèque de graphiques |
| **react-chartjs-2** | 5.3.1 | Wrapper React pour Chart.js |
| **Lucide React** | 0.445.0 | Bibliothèque d'icônes |
| **Three.js** | 0.181.2 | Effets 3D WebGL (page de connexion) |
| **Playwright** | 1.58.2 | Tests end-to-end |

### 4.3 WhatsApp Bridge (Microservice)

| Technologie | Version | Utilisation |
|-------------|---------|-------------|
| **Node.js** | 18 | Runtime JavaScript côté serveur |
| **Express** | 4.18.2 | Framework web HTTP |
| **whatsapp-web.js** | 1.23.0 | Client WhatsApp Web non-officiel |
| **qrcode** | 1.5.4 | Génération de QR codes |
| **Chromium** | (headless) | Navigateur pour whatsapp-web.js (via Puppeteer) |

### 4.4 DevOps et Déploiement

| Technologie | Utilisation |
|-------------|-------------|
| **Docker** | Containerisation des services |
| **Docker Compose** | Orchestration multi-conteneurs |
| **Nginx** | Reverse proxy (optionnel) |
| **Multi-stage Build** | Optimisation des images Docker (3 étapes) |

---

## 5. Architecture Backend (Spring Boot)

### 5.1 Couche API (Controllers)

Le backend expose **15 contrôleurs REST** totalisant plus de **60 endpoints** :

| Contrôleur | Préfixe | Endpoints | Responsabilité |
|------------|---------|-----------|----------------|
| `AuthController` | `/api/auth` | 4 | Inscription, connexion, refresh token, changement de mot de passe |
| `OrderController` | `/api/orders` | 10 | CRUD commandes, historique, envoi au transporteur |
| `StoreController` | `/api/stores` | 12 | CRUD boutiques, paramètres WhatsApp, revenus support |
| `ShippingController` | `/api/shipping` | 11 | Fournisseurs de livraison, colis Ozon Express, bons de livraison |
| `UserController` | `/api/users` | 7 | Gestion utilisateurs, ban/unban |
| `TeamController` | `/api/stores/{id}/team` | 7 | Équipes par boutique, invitations, rôles |
| `YouCanController` | `/api/youcan` | 5 | OAuth YouCan, synchronisation commandes |
| `ExcelSyncController` | `/api/excel-sync` | 6 | Synchronisation Google Sheets |
| `FileUploadController` | `/api/stores/{id}/upload` | 1 | Import fichiers Excel |
| `WebhookController` | `/api/webhooks` | 5 | Abonnements webhook |
| `ApiCredentialController` | `/api/credentials` | 4 | Gestion des clés API |
| `PublicApiController` | `/api/public` | 2 | API publique (auth par clé API) |
| `SupportController` | `/api/support` | 6 | Revenus et demandes de paiement du support |
| `WhatsAppController` | `/api/whatsapp` | 1 | Statut de liaison WhatsApp |
| `HealthController` | `/api/health` | 1 | Health check |

### 5.2 Couche DTO (Data Transfer Objects)

Les DTOs assurent la séparation entre les entités JPA et les données exposées via l'API :

| DTO | Champs Principaux | Validation |
|-----|-------------------|------------|
| `SignupRequest` | email, password, firstName, lastName, phoneNumber, accountName, accountType | @Email, @NotBlank, @Size(min=8) |
| `AuthRequest` | email, password | @Email, @NotBlank |
| `AuthResponse` | accessToken, refreshToken, tokenType, expiresIn, user | - |
| `UserDto` | id, email, firstName, lastName, role, status, mustChangePassword, selectedStoreId | - |
| `StoreDto` | id, name, code, accountId, managerId, managerName, status, timezone, logoUrl, color | - |
| `OrderDto` | id, storeId, source, customerName/Phone, city, status, totalAmount, ozonTrackingNumber, deliveryNoteRef, assignedToName | - |
| `TeamMemberDto` | id, storeId, userId, email, role, invitationStatus | - |
| `ExcelSyncConfigDto` | id, storeId, spreadsheetId, sheetName, syncEnabled, columnMapping, lastSyncStatus | - |

### 5.3 Couche Mappers

Les mappers sont des `@Component` Spring avec des méthodes `toDto()` manuelles :

| Mapper | Logique Notable |
|--------|-----------------|
| `OrderMapper` | Extraction de `city`, `productName`, `productId` depuis le champ JSON `metadata` en fallback. Résolution du nom de l'utilisateur assigné. |
| `StoreMapper` | Construction du `managerName` depuis `firstName` + `lastName` du manager. |
| `UserMapper` | Mapping de `mustChangePassword` et `selectedStoreId`. |
| `TeamMapper` | Gestion du cas où le `user` est null (membre externe invité). |

### 5.4 Couche Domaine (Entités JPA)

Le domaine comprend **12 entités principales** :

```
Account (Compte)
  │
  ├── User (Utilisateur)
  │     • Rôles : PLATFORM_ADMIN, ACCOUNT_OWNER, MANAGER, SUPPORT
  │     • Statuts : INVITED, ACTIVE, DISABLED, BANNED
  │
  ├── Store (Boutique)
  │     • Statuts : ACTIVE, DISABLED, ARCHIVED, DELETED
  │     • Code unique auto-généré (ex: ST-TAL-1710000000)
  │     │
  │     ├── StoreSettings (Paramètres)
  │     │     • WhatsApp automation, templates, tarification support
  │     │
  │     ├── StoreTeamMember (Membre d'équipe)
  │     │     • Rôles : MANAGER, SUPPORT, EXTERNAL_SUPPORT
  │     │     • Invitation : PENDING, ACCEPTED, REJECTED
  │     │
  │     └── Order (Commande)
  │           • Sources : MANUAL, EXCEL_UPLOAD, API, WEBHOOK, YOUCAN
  │           • Statuts : ENCOURS, CONFIRMED, CONCLED, APPEL_1, APPEL_2
  │           │
  │           └── OrderStatusHistory (Historique)
  │
  ├── ShippingProvider (Fournisseur de livraison)
  │     • Type : OZON_EXPRESS
  │
  ├── ApiCredential (Clé API)
  │     • publicKey + secretKeyHash
  │
  └── WebhookSubscription (Abonnement webhook)
        • URL cible, événements, token secret
```

### 5.5 Couche Services (Logique Métier)

#### OrderService - Gestion des Commandes

```
Création de commande
    │
    ├── Vérification de doublon (externalOrderId)
    ├── Sauvegarde en BDD
    └── Distribution automatique aux équipes support
         └── Round-robin par ancienneté (createdAt ASC)

Mise à jour de statut
    │
    ├── Vérification des permissions
    ├── Enregistrement dans l'historique
    ├── Attribution automatique à l'agent support
    ├── Si CONFIRMED → Crédit du revenu support
    └── Si CONFIRMED/CONCLED → Envoi WhatsApp automatique
```

#### StoreService - Gestion des Boutiques

- Création avec génération de code unique (`ST-{prefix}-{timestamp}`)
- Visibilité basée sur le rôle : OWNER voit tout, MANAGER voit ses boutiques, SUPPORT voit les boutiques de ses équipes
- Support cross-account pour les membres d'équipe

#### OzonExpressService - Intégration Transporteur

```
Envoi au transporteur
    │
    ├── Création du colis (POST /parcels)
    │     • Mapping ville → ID ville Ozon Express
    │     • Formatage prix (entier uniquement)
    │
    ├── Suivi en temps réel (POST /parcels/track)
    │
    └── Bon de Livraison (Delivery Note)
          ├── Création du BL
          ├── Ajout des colis
          ├── Finalisation
          └── Téléchargement PDF (proxy)
```

---

## 6. Architecture Frontend (React)

### 6.1 Structure des Fichiers

```
talabaty-frontend/src/
├── App.tsx                    # Routes et providers
├── pages/                     # 17 pages
│   ├── Login.tsx              #   Connexion (avec effet 3D WebGL)
│   ├── Signup.tsx             #   Inscription
│   ├── Dashboard.tsx          #   Tableau de bord analytique
│   ├── Orders.tsx             #   Liste des commandes (1016 lignes)
│   ├── OrderDetail.tsx        #   Détail d'une commande
│   ├── Stores.tsx             #   Liste des boutiques
│   ├── StoreDetail.tsx        #   Détail d'une boutique
│   ├── ShippingProviders.tsx  #   Fournisseurs de livraison
│   ├── Integrations.tsx       #   Hub d'intégrations
│   ├── Automations.tsx        #   Automatisation WhatsApp
│   ├── Users.tsx              #   Gestion des utilisateurs
│   ├── Earnings.tsx           #   Gains du support
│   ├── PaymentRequests.tsx    #   Demandes de paiement
│   ├── SelectStore.tsx        #   Sélection de boutique
│   ├── ChangePassword.tsx     #   Changement de mot de passe
│   ├── Settings.tsx           #   Paramètres (onglets)
│   └── Help.tsx               #   Aide
├── components/                # 28 composants
│   ├── layout/                #   Header, Sidebar, Layout
│   ├── dashboard/             #   StatCard, ChartCard, graphiques
│   └── *.tsx                  #   Modales, formulaires, UI
├── services/                  # 11 services API
│   ├── api.ts                 #   Instance Axios configurée
│   ├── authService.ts         #   Authentification
│   ├── orderService.ts        #   Commandes
│   ├── storeService.ts        #   Boutiques
│   ├── shippingService.ts     #   Livraison
│   ├── teamService.ts         #   Équipes
│   ├── userService.ts         #   Utilisateurs
│   ├── youcanService.ts       #   Intégration YouCan
│   ├── googleSheetsSyncService.ts  #   Google Sheets
│   ├── supportService.ts      #   Support/revenus
│   └── whatsappService.ts     #   WhatsApp
├── utils/                     # Utilitaires
│   ├── permissions.ts         #   Permissions RBAC côté client
│   ├── cityMapping.ts         #   Mapping 630+ villes marocaines
│   ├── deliveryCities.ts      #   Validation villes de livraison
│   ├── storeColors.ts         #   Thème dynamique par boutique
│   └── deliveryProviders.ts   #   Configuration 11 transporteurs
├── hooks/                     # Custom hooks
│   └── useStoreColor.ts       #   Hook couleur de boutique
├── constants/                 # Constantes
│   └── brand.ts               #   Couleurs de la marque
└── context/                   # Contextes React
    ├── AuthContext.tsx         #   État d'authentification
    └── ThemeContext.tsx        #   Mode sombre/clair
```

### 6.2 Système de Routage

```
/login                  → Login          (Public)
/signup                 → Signup         (Public)
/change-password        → ChangePassword (Protégé, sans boutique requise)
/select-store           → SelectStore    (Protégé, sans boutique requise)

/                       → Dashboard      (Protégé + Boutique requise)
/stores                 → Stores         │
/stores/:id             → StoreDetail    │
/orders                 → Orders         │  Toutes ces routes
/orders/:id             → OrderDetail    │  nécessitent une
/shipping               → ShippingProviders │  authentification
/integrations           → Integrations   │  ET une boutique
/automations            → Automations    │  sélectionnée
/users                  → Users          │
/earnings               → Earnings       │
/payment-requests       → PaymentRequests│
/help                   → Help           │
/settings               → Settings       │
```

**Logique du `ProtectedRoute`** :
1. Si chargement → Afficher le spinner Talabaty
2. Si non authentifié → Rediriger vers `/login`
3. Si `mustChangePassword` → Rediriger vers `/change-password`
4. Si pas de boutique sélectionnée → Rediriger vers `/select-store`
5. Sinon → Afficher la page demandée

### 6.3 Système de Thème Dynamique

Chaque boutique possède une couleur personnalisée qui s'applique à l'ensemble de l'interface :

```
Couleur de boutique (ex: #0284c7)
    │
    ├── Boutons primaires (fond coloré, texte blanc)
    ├── Liens actifs dans la sidebar
    ├── Bordures et accents des cartes
    ├── Couleurs des graphiques (Chart.js)
    ├── Badges et indicateurs de statut
    └── Hover states (assombri/éclairci dynamiquement)
```

Le hook `useStoreColor()` récupère la couleur depuis l'API et l'injecte via des `style` inline. Le mode sombre est géré via `ThemeContext` avec la classe CSS `dark` de Tailwind.

### 6.4 Composants UI Notables

**Aucune bibliothèque de composants externe** (pas de MUI, Ant Design, Chakra) : tous les composants sont construits sur mesure avec Tailwind CSS, ce qui offre un contrôle total sur le design.

#### GridDistortion (Effet 3D Login)
Effet WebGL interactif utilisant Three.js qui crée une distorsion de grille réactive au mouvement de la souris sur la page de connexion.

#### TalabatyLogoSpinner
Spinner de chargement personnalisé utilisant le logo SVG Talabaty avec une animation CSS de rotation.

---

## 7. Base de Données

### 7.1 Technologie : PostgreSQL 16

- **ORM** : Hibernate (via Spring Data JPA)
- **Migrations** : Liquibase (6 fichiers de changelog, 19 changesets)
- **Mode DDL** : `none` (le schéma est entièrement géré par Liquibase)

### 7.2 Schéma de la Base de Données

**18 tables** avec **26 relations de clé étrangère** :

```
┌──────────────────┐
│    accounts       │
│  (Comptes)        │─────────────────────────────────────────┐
│  id (PK, UUID)    │                                         │
│  name             │                                         │
│  type             │     ┌──────────────────┐                │
│  status           │     │     users         │                │
│  timezone         │────▶│  (Utilisateurs)   │                │
│  metadata (JSONB) │     │  id (PK, UUID)    │                │
└──────────────────┘     │  account_id (FK)  │                │
                          │  email (UNIQUE)   │                │
        ┌─────────────────│  password_hash    │                │
        │                 │  role, status     │                │
        │                 │  must_change_pwd  │                │
        │                 │  selected_store_id│                │
        │                 └──────────────────┘                │
        │                                                      │
        ▼                                                      │
┌──────────────────┐     ┌──────────────────┐                │
│    stores         │     │  store_settings   │                │
│  (Boutiques)      │────▶│  (Paramètres)     │                │
│  id (PK, UUID)    │  1:1│  store_id (PK/FK) │                │
│  account_id (FK)  │◀────│  whatsapp_*       │                │
│  name, code       │     │  price_per_order_*│                │
│  manager_id (FK)  │     └──────────────────┘                │
│  logo_url, color  │                                         │
└───────┬──────────┘                                         │
        │                                                      │
        ├─────────────────────────────────┐                   │
        │                                 │                   │
        ▼                                 ▼                   │
┌──────────────────┐     ┌──────────────────┐                │
│     orders        │     │store_team_members │                │
│  (Commandes)      │     │  (Membres)        │                │
│  id (PK, UUID)    │     │  id (PK, UUID)    │                │
│  store_id (FK)    │     │  store_id (FK)    │                │
│  source           │     │  user_id (FK)     │                │
│  customer_*       │     │  role             │                │
│  city, status     │     │  invitation_status│                │
│  total_amount     │     └──────────────────┘                │
│  ozon_tracking_no │                                         │
│  delivery_note_ref│     ┌──────────────────┐                │
│  assigned_to (FK) │     │shipping_providers │                │
│  metadata (JSONB) │     │  id (PK, UUID)    │                │
└───────┬──────────┘     │  account_id (FK)  │◀───────────────┘
        │                 │  store_id (FK)    │
        ▼                 │  provider_type    │
┌──────────────────┐     │  customer_id      │
│order_status_hist. │     │  api_key          │
│  (Historique)     │     └──────────────────┘
│  id (PK, UUID)    │
│  order_id (FK)    │     ┌──────────────────┐
│  status           │     │  youcan_stores    │
│  changed_by (FK)  │     │  id (PK, UUID)    │
│  changed_at       │     │  account_id (FK)  │
└──────────────────┘     │  store_id (FK)    │
                          │  access_token     │
                          │  last_sync_at     │
                          └──────────────────┘
```

### 7.3 Tables Complètes

| Table | Colonnes | Description |
|-------|----------|-------------|
| `accounts` | 9 | Comptes (tenants) avec type INDIVIDUAL/COMPANY |
| `users` | 15 | Utilisateurs avec 4 rôles et 4 statuts |
| `stores` | 10 | Boutiques avec code unique, logo et couleur |
| `store_settings` | 14 | Paramètres par boutique (WhatsApp, tarification) |
| `store_team_members` | 9 | Membres d'équipe avec rôles et invitations |
| `orders` | 18 | Commandes avec 5 sources et 5 statuts |
| `order_status_history` | 6 | Historique des changements de statut |
| `order_import_batches` | 11 | Lots d'importation Excel |
| `order_import_rows` | 9 | Lignes d'importation détaillées |
| `stored_files` | 8 | Fichiers uploadés |
| `shipping_providers` | 10 | Fournisseurs de livraison (Ozon Express) |
| `api_credentials` | 7 | Clés API publiques/secrètes |
| `webhook_subscriptions` | 7 | Abonnements webhook |
| `webhook_subscription_events` | 2 | Événements par abonnement (table de jointure) |
| `youcan_stores` | 14 | Boutiques YouCan connectées (OAuth) |
| `excel_sync_configs` | 17 | Configurations de sync Google Sheets |
| `support_revenue_entries` | 7 | Entrées de revenus des agents support |
| `support_payment_requests` | 9 | Demandes de paiement des agents support |

### 7.4 Gestion des Migrations avec Liquibase

Les migrations sont versionnées dans des fichiers YAML :

| Fichier | Changesets | Description |
|---------|------------|-------------|
| `0001-initial-schema.yaml` | 13 | Schéma initial complet (accounts → webhooks) |
| `0016-add-user-extra-columns.yaml` | 1 | phoneNumber, mustChangePassword, selectedStoreId |
| `0017-schema-sync-with-entities.yaml` | 1 | ozonTrackingNumber, city, productName, assignedTo, shipping_providers, youcan_stores, excel_sync_configs |
| `0018-add-delivery-note-ref.yaml` | 1 | deliveryNoteRef sur orders |
| `0019-whatsapp-automation.yaml` | 1 | Colonnes WhatsApp sur store_settings |
| `0020-support-revenue-and-prices.yaml` | 2 | Tables support_revenue_entries et support_payment_requests, tarification par commande |

---

## 8. Sécurité et Authentification

### 8.1 Architecture de Sécurité

```
Requête HTTP
    │
    ├── [1] ApiKeyAuthenticationFilter
    │     └── Vérifie X-API-Key + X-API-Secret (pour API publique)
    │
    ├── [2] JwtAuthenticationFilter
    │     └── Extrait et valide le Bearer Token JWT
    │
    └── [3] Spring Security Filter Chain
          └── Vérifie les autorisations par endpoint
```

### 8.2 Authentification JWT (JSON Web Token)

**Algorithme** : HMAC-SHA256

| Type de Token | Durée | Claims |
|---------------|-------|--------|
| Access Token | 1 heure (3600s) | userId, email, accountId |
| Refresh Token | 24 heures (86400s) | userId (subject uniquement) |

**Flux d'authentification** :

```
1. Login (POST /api/auth/login)
   └── Email + Password → Vérification BCrypt → Génération tokens

2. Requête authentifiée
   └── Header: Authorization: Bearer <accessToken>
   └── JwtAuthenticationFilter extrait et valide le token
   └── Crée JwtUser (userId, accountId, email) dans le SecurityContext

3. Refresh Token (POST /api/auth/refresh)
   └── refreshToken → Validation → Nouveau accessToken

4. Changement de mot de passe obligatoire
   └── Si mustChangePassword = true → Force la redirection
   └── Premier changement : pas besoin de l'ancien mot de passe
```

### 8.3 Authentification par Clé API

Pour l'API publique (`/api/public/*`), une authentification alternative par clé API est disponible :

```
Headers requis :
  X-API-Key: <publicKey>
  X-API-Secret: <secretKey>

→ Validation contre la BDD (api_credentials)
→ Crée un contexte avec accountId uniquement (pas de user)
```

### 8.4 RBAC (Role-Based Access Control)

**4 rôles hiérarchiques** :

```
PLATFORM_ADMIN (Super administrateur)
    │  → Accès total à toutes les fonctionnalités
    │
ACCOUNT_OWNER (Propriétaire du compte)
    │  → Gestion complète de son compte et ses boutiques
    │
MANAGER (Gestionnaire de boutique)
    │  → Gestion de ses boutiques assignées
    │
SUPPORT (Agent de support)
       → Traitement des commandes assignées uniquement
```

**Matrice des permissions** :

| Permission | PLATFORM_ADMIN | ACCOUNT_OWNER | MANAGER | SUPPORT |
|------------|:-:|:-:|:-:|:-:|
| Créer boutique | ✅ | ✅ | ❌ | ❌ |
| Modifier boutique | ✅ | ✅ | ✅ (les siennes) | ❌ |
| Supprimer boutique | ✅ | ✅ | ❌ | ❌ |
| Gérer utilisateurs | ✅ | ✅ | ❌ | ❌ |
| Gérer équipe | ✅ | ✅ | ✅ (ses boutiques) | ❌ |
| Créer commande | ✅ | ✅ | ✅ | ✅ |
| Modifier commande | ✅ | ✅ | ✅ | ✅ (non assignées/siennes) |
| Gérer livraison | ✅ | ✅ | ✅ | ❌ |
| Intégrations | ✅ | ✅ | ✅ | ❌ |
| Demandes de paiement | ✅ | ✅ | ❌ | ❌ |

### 8.5 Chiffrement

- **Mots de passe** : BCrypt (salage automatique, coût par défaut 10)
- **Tokens JWT** : HMAC-SHA256 avec clé secrète configurable
- **Clés API** : Hash SHA-256 du secret stocké en BDD

### 8.6 Sécurité Additionnelle

- **Sessions stateless** : pas de session côté serveur, tout passe par JWT
- **CORS configuré** : origines autorisées explicitement listées
- **Validation des entrées** : Jakarta Bean Validation (@Email, @NotBlank, @Size)
- **Gestion d'erreurs centralisée** : `GlobalExceptionHandler` avec codes d'erreur standardisés
- **Verrouillage optimiste** : champ `version` sur `Account` pour prévenir les conflits de mise à jour

---

## 9. Fonctionnalités Détaillées

### 9.1 Gestion des Commandes

#### Cycle de vie d'une commande

```
ENCOURS (En cours)
    │
    ├──→ APPEL_1 (Premier appel de confirmation)
    │       │
    │       └──→ APPEL_2 (Deuxième appel)
    │               │
    │               └──→ CONFIRMED (Confirmé) ──→ Envoi au transporteur
    │                       │                         │
    │                       │                         └──→ Suivi en temps réel
    │                       │
    │                       └──→ CONCLED (Annulé/Refusé)
    │
    └──→ CONFIRMED (Confirmation directe)
    │
    └──→ CONCLED (Annulation directe)
```

#### Sources de commandes

| Source | Méthode d'importation | Description |
|--------|----------------------|-------------|
| `MANUAL` | Interface web | Création manuelle via formulaire |
| `EXCEL_UPLOAD` | Upload de fichier .xlsx | Import en masse depuis Excel |
| `YOUCAN` | OAuth + Synchronisation auto | Sync depuis la plateforme e-commerce YouCan |
| `API` | API publique (clé API) | Création programmatique par des systèmes tiers |
| `WEBHOOK` | Webhook entrant | Réception automatique via webhook |

#### Distribution automatique des commandes

Les commandes non assignées sont automatiquement distribuées aux membres de l'équipe support par un algorithme **round-robin** basé sur l'ancienneté de la dernière commande assignée.

### 9.2 Intégration Ozon Express (Livraison)

#### Fonctionnalités

| Fonctionnalité | Description |
|----------------|-------------|
| **Création de colis** | Envoi automatique au transporteur avec mapping ville → ID ville |
| **Suivi en temps réel** | Tracking par numéro de colis, unitaire ou en masse |
| **Bon de Livraison (BL)** | Création, ajout de colis, finalisation, téléchargement PDF |
| **Liste des villes** | 630+ villes marocaines avec tarification |
| **Envoi par lot** | Envoi de plusieurs commandes en une seule opération |

#### Mapping des villes marocaines

Le système inclut un mapping intelligent de 630+ villes avec :
- Correspondance exacte (`Casablanca` → ID)
- Alias (`Casa`, `Dar el Beida` → Casablanca)
- Correspondance partielle (recherche fuzzy)
- Nettoyage automatique (suppression préfixes/suffixes)

### 9.3 Automatisation WhatsApp

#### Deux modes de fonctionnement

| Mode | Coût | Technologie | Configuration |
|------|------|-------------|---------------|
| **Bridge local** (gratuit) | Gratuit | whatsapp-web.js + Chromium | Scan du QR code depuis l'interface |
| **Twilio** (payant) | ~0.005$/msg | API Twilio WhatsApp | Account SID + Auth Token |

#### Messages automatiques

Les messages sont envoyés automatiquement lors des transitions de statut :
- **CONFIRMED** → Message de confirmation avec détails de commande
- **CONCLED** → Message de notification d'annulation

#### Variables de template disponibles

| Variable | Description | Exemple |
|----------|-------------|---------|
| `{{customerName}}` | Nom du client | Ahmed Benali |
| `{{orderId}}` | ID de la commande | 8f3a2b... |
| `{{trackingNumber}}` | Numéro de suivi | OZ123456 |
| `{{totalAmount}}` | Montant total | 250.00 |
| `{{currency}}` | Devise | MAD |
| `{{city}}` | Ville de destination | Casablanca |

#### Exemple de template par défaut

```
Bonjour {{customerName}} 👋

Votre commande a été confirmée ✅
Numéro de suivi : {{trackingNumber}}
Montant : {{totalAmount}} {{currency}}
Ville : {{city}}

Merci pour votre confiance ! 🙏

---

مرحبا {{customerName}} 👋
تم تأكيد طلبك ✅
رقم التتبع: {{trackingNumber}}
```

#### Diffusion promotionnelle

Possibilité d'envoyer un message promotionnel à tous les clients uniques d'une boutique en un seul clic, avec :
- Comptage des destinataires avant envoi
- Confirmation de l'utilisateur
- Rapport de résultats (envoyés/échoués)

### 9.4 Gestion Multi-Boutique

```
Compte (Account)
    │
    ├── Boutique 1 (Store)
    │     ├── Couleur : #0284c7 (bleu)
    │     ├── Logo personnalisé
    │     ├── Équipe dédiée
    │     ├── Commandes propres
    │     ├── Fournisseur de livraison
    │     └── Session WhatsApp indépendante
    │
    ├── Boutique 2 (Store)
    │     ├── Couleur : #FF6E00 (orange)
    │     ├── Logo différent
    │     ├── Autre équipe
    │     └── ...
    │
    └── Boutique N...
```

Chaque boutique est **complètement indépendante** avec :
- Sa propre identité visuelle (couleur, logo)
- Son propre code unique
- Son équipe de managers et support
- Ses commandes séparées
- Sa configuration de livraison
- Sa session WhatsApp dédiée
- Ses paramètres de tarification support

### 9.5 Dashboard Analytique

#### KPIs affichés

| KPI | Description | Visualisation |
|-----|-------------|---------------|
| Total commandes | Nombre de commandes sur la période | Carte + tendance |
| Commandes confirmées | Nombre de commandes au statut CONFIRMED | Carte avec pourcentage |
| Commandes livrées | Commandes CONCLED (livrées) | Carte avec pourcentage |
| Commandes en cours | Commandes ENCOURS | Carte |
| Commandes annulées | Commandes refusées | Carte |
| Taux de succès | (Confirmées / Total) × 100 | Carte en pourcentage |
| Revenu | Somme des montants des commandes confirmées | Carte en MAD |

#### Graphiques

| Graphique | Type | Données |
|-----------|------|---------|
| Commandes par jour | Barres (Bar Chart) | Nombre de commandes par date |
| Revenu dans le temps | Lignes (Line Chart) | Montant cumulé par date |
| Commandes par ville | Barres horizontales | Distribution géographique |

Tous les graphiques sont dynamiquement colorés avec la couleur de la boutique sélectionnée.

### 9.6 Système de Rémunération du Support

```
Configuration (par boutique) :
    • Prix par commande confirmée : X MAD
    • Prix par commande livrée : Y MAD

Flux :
    Agent support confirme une commande
        │
        └──→ Crédit automatique de X MAD sur son solde
        
    Agent consulte ses gains (page Earnings)
        │
        ├── Solde actuel = Total gagné - Total payé
        └── Historique des demandes de paiement

    Agent demande un paiement
        │
        └──→ Validation (montant ≤ solde)
              │
              └──→ Statut PENDING → Admin marque PAID ou REJECTED
```

### 9.7 Import Excel et Google Sheets

#### Import Excel (Upload)

- Upload de fichiers `.xlsx` (max 10MB)
- Parsing via Apache POI
- Suivi par lot (batch) avec statut par ligne
- Détection de doublons par `externalOrderId`

#### Synchronisation Google Sheets

- Configuration par boutique : spreadsheet ID, nom de la feuille, mapping des colonnes
- Synchronisation manuelle ou automatique (intervalle configurable)
- Scheduler automatique avec `@EnableScheduling`
- Gestion des erreurs avec statut de dernière synchronisation

### 9.8 API Publique

Endpoints accessibles via clé API (sans JWT) :

```
POST /api/public/orders          → Créer une commande
GET  /api/public/orders/{id}/status → Consulter le statut

Headers requis :
  X-API-Key: <publicKey>
  X-API-Secret: <secretKey>
```

### 9.9 Système de Webhooks

Abonnements aux événements avec notification automatique :
- Configuration d'URL cible
- Sélection des types d'événements
- Token secret pour la vérification
- Activation/désactivation

---

## 10. Intégrations Externes

### 10.1 YouCan (E-commerce)

**Type** : OAuth 2.0

```
Flux OAuth :
    1. Utilisateur clique "Connecter YouCan"
    2. Redirection vers seller-area.youcan.shop/admin/oauth/authorize
    3. Utilisateur autorise l'accès
    4. Callback avec code d'autorisation
    5. Échange du code contre access_token + refresh_token
    6. Stockage des tokens en BDD

Synchronisation :
    • Manuelle : bouton "Sync" par boutique YouCan
    • Automatique : scheduler périodique
    • Détection de doublons par externalOrderId
```

### 10.2 Google Sheets

**Type** : API Google Sheets v4

```
Configuration :
    • Spreadsheet ID (extrait de l'URL Google Sheets)
    • Nom de la feuille (ex: "Sheet1")
    • Mapping des colonnes (JSON personnalisable)
    • Intervalle de synchronisation (par défaut 30 secondes)
    
Authentification Google :
    • Credentials JSON (service account) OU
    • Access Token + Refresh Token (OAuth)
```

### 10.3 Ozon Express (Livraison)

**Type** : API REST avec authentification par Customer ID + API Key

Endpoints intégrés :
- Création de colis
- Suivi de colis (unitaire et en masse)
- Information de colis
- Gestion des bons de livraison (CRUD + PDF)
- Liste des villes et tarification

### 10.4 WhatsApp (Communication)

**Deux options** :
1. **whatsapp-web.js** (gratuit) : Service Node.js local simulant WhatsApp Web
2. **Twilio** (payant) : API officielle WhatsApp Business

---

## 11. Microservice WhatsApp Bridge

### 11.1 Architecture

Le WhatsApp Bridge est un **microservice Node.js indépendant** qui encapsule la bibliothèque `whatsapp-web.js` :

```
┌─────────────────────────────────────────┐
│          WhatsApp Bridge                 │
│         (Node.js + Express)              │
│                                          │
│  ┌──────────────────────────────────┐   │
│  │        Express Server             │   │
│  │         Port 3100                 │   │
│  │                                   │   │
│  │  GET  /qr          → QR Code     │   │
│  │  GET  /status      → État        │   │
│  │  POST /send        → Envoi msg   │   │
│  │  POST /send-bulk   → Envoi masse │   │
│  └───────────┬──────────────────────┘   │
│              │                           │
│  ┌───────────▼──────────────────────┐   │
│  │     whatsapp-web.js Client        │   │
│  │  (Simule WhatsApp Web)            │   │
│  │                                   │   │
│  │  • Authentification locale        │   │
│  │  • Sessions multiples             │   │
│  │  • Auto-reconnexion               │   │
│  │  • Génération QR → Data URL       │   │
│  └───────────┬──────────────────────┘   │
│              │                           │
│  ┌───────────▼──────────────────────┐   │
│  │     Chromium (Headless)           │   │
│  │  (Puppeteer sous le capot)        │   │
│  └──────────────────────────────────┘   │
└─────────────────────────────────────────┘
```

### 11.2 Fonctionnalités

| Fonctionnalité | Description |
|----------------|-------------|
| **Multi-session** | Chaque boutique a sa propre session WhatsApp (`sessionId`) |
| **QR Code** | Génération de QR code en Data URL pour liaison dans le navigateur |
| **Auto-reconnexion** | Détection de déconnexion et redémarrage automatique |
| **Watchdog** | Timer de 8 secondes pour forcer le restart si le process est bloqué |
| **Envoi en masse** | Envoi à plusieurs destinataires avec délai de 1.5s entre chaque |
| **Normalisation** | Formatage automatique des numéros marocains (suppression +, ajout @c.us) |
| **Résilience** | Handlers `uncaughtException` et `unhandledRejection` pour la stabilité |

### 11.3 Flux de Liaison

```
1. Utilisateur ouvre la page "Automations"
2. Frontend appelle GET /stores/{id}/whatsapp-link-status toutes les 2.5s
3. Backend appelle le Bridge GET /qr?sessionId={storeId}
4. Bridge retourne le QR code en Data URL
5. Frontend affiche le QR code
6. Utilisateur scanne avec son téléphone WhatsApp
7. Bridge détecte la connexion réussie
8. GET /qr retourne { ready: true }
9. Frontend affiche "Connecté" ✅
```

---

## 12. API REST Complète

### 12.1 Authentification

| Méthode | Endpoint | Auth | Description |
|---------|----------|------|-------------|
| `POST` | `/api/auth/signup` | Publique | Inscription (compte + utilisateur) |
| `POST` | `/api/auth/login` | Publique | Connexion |
| `POST` | `/api/auth/refresh` | Publique | Rafraîchir le token |
| `POST` | `/api/auth/change-password` | JWT | Changer le mot de passe |

### 12.2 Commandes

| Méthode | Endpoint | Auth | Description |
|---------|----------|------|-------------|
| `POST` | `/api/orders` | JWT | Créer une commande |
| `GET` | `/api/orders/store/{storeId}` | JWT | Lister les commandes d'une boutique |
| `GET` | `/api/orders/{id}` | JWT | Détail d'une commande |
| `PUT` | `/api/orders/{id}` | JWT | Modifier une commande |
| `PUT` | `/api/orders/{id}/status` | JWT | Changer le statut |
| `GET` | `/api/orders/{id}/history` | JWT | Historique des changements |
| `GET` | `/api/orders/store/{storeId}/status/{status}` | JWT | Filtrer par statut |
| `POST` | `/api/orders/{id}/send-to-shipping` | JWT | Envoyer au transporteur |
| `POST` | `/api/orders/batch/send-to-shipping` | JWT | Envoi en masse |

### 12.3 Boutiques

| Méthode | Endpoint | Auth | Description |
|---------|----------|------|-------------|
| `POST` | `/api/stores` | JWT | Créer une boutique |
| `GET` | `/api/stores` | JWT | Lister les boutiques |
| `GET` | `/api/stores/{id}` | JWT | Détail d'une boutique |
| `PUT` | `/api/stores/{id}` | JWT | Modifier une boutique |
| `DELETE` | `/api/stores/{id}` | JWT | Supprimer une boutique |
| `POST` | `/api/stores/{id}/shipping-providers` | JWT | Ajouter un transporteur |
| `GET` | `/api/stores/{id}/shipping-providers` | JWT | Lister les transporteurs |
| `GET` | `/api/stores/{id}/whatsapp-settings` | JWT | Paramètres WhatsApp |
| `PATCH` | `/api/stores/{id}/whatsapp-settings` | JWT | Modifier paramètres WhatsApp |
| `GET` | `/api/stores/{id}/whatsapp-link-status` | JWT | Statut liaison WhatsApp |
| `POST` | `/api/stores/{id}/whatsapp-broadcast` | JWT | Diffusion promotionnelle |
| `GET` | `/api/stores/{id}/support-revenue-settings` | JWT | Tarification support |
| `PATCH` | `/api/stores/{id}/support-revenue-settings` | JWT | Modifier tarification |

### 12.4 Livraison (Ozon Express)

| Méthode | Endpoint | Auth | Description |
|---------|----------|------|-------------|
| `POST` | `/api/shipping/providers` | JWT | Créer un fournisseur |
| `GET` | `/api/shipping/providers` | JWT | Lister les fournisseurs |
| `PUT` | `/api/shipping/providers/{id}` | JWT | Modifier un fournisseur |
| `DELETE` | `/api/shipping/providers/{id}` | JWT | Supprimer un fournisseur |
| `POST` | `/api/shipping/ozon-express/parcels` | JWT | Créer un colis |
| `POST` | `/api/shipping/ozon-express/parcels/info` | JWT | Info d'un colis |
| `POST` | `/api/shipping/ozon-express/parcels/track` | JWT | Suivre des colis |
| `POST` | `/api/shipping/ozon-express/delivery-notes` | JWT | Créer un bon de livraison |
| `POST` | `/api/shipping/ozon-express/delivery-notes/create-full` | JWT | Créer BL complet (one-step) |
| `GET` | `/api/shipping/ozon-express/delivery-notes/pdf` | JWT | Télécharger PDF du BL |
| `GET` | `/api/shipping/ozon-express/cities` | JWT | Liste des villes |

### 12.5 Utilisateurs et Équipes

| Méthode | Endpoint | Auth | Description |
|---------|----------|------|-------------|
| `GET` | `/api/users` | JWT | Lister les utilisateurs |
| `POST` | `/api/users` | JWT | Créer un utilisateur |
| `PUT` | `/api/users/{id}/ban` | JWT | Bannir |
| `PUT` | `/api/users/{id}/unban` | JWT | Débannir |
| `POST` | `/api/stores/{id}/team/create-member` | JWT | Créer un membre d'équipe |
| `POST` | `/api/stores/{id}/team/bulk-create` | JWT | Création en masse |
| `GET` | `/api/stores/{id}/team` | JWT | Lister l'équipe |
| `PUT` | `/api/stores/{id}/team/{mid}/role` | JWT | Changer le rôle |
| `DELETE` | `/api/stores/{id}/team/{mid}` | JWT | Retirer un membre |

### 12.6 Intégrations

| Méthode | Endpoint | Auth | Description |
|---------|----------|------|-------------|
| `GET` | `/api/youcan/connect/{storeId}` | JWT | Initier OAuth YouCan |
| `GET` | `/api/youcan/oauth/callback` | Publique | Callback OAuth |
| `POST` | `/api/youcan/stores/{id}/sync` | JWT | Synchroniser commandes |
| `POST` | `/api/excel-sync` | JWT | Créer config Google Sheets |
| `POST` | `/api/excel-sync/{id}/sync` | JWT | Déclencher sync manuelle |

### 12.7 Support et Paiements

| Méthode | Endpoint | Auth | Description |
|---------|----------|------|-------------|
| `GET` | `/api/support/balance` | JWT | Solde de l'agent support |
| `POST` | `/api/support/request-payment` | JWT | Demander un paiement |
| `GET` | `/api/support/payment-requests` | JWT | Mes demandes de paiement |
| `GET` | `/api/support/payment-requests/admin` | JWT | Toutes les demandes (admin) |
| `PUT` | `/api/support/payment-requests/{id}/paid` | JWT | Marquer comme payé |
| `PUT` | `/api/support/payment-requests/{id}/rejected` | JWT | Rejeter la demande |

### 12.8 API Publique (Clé API)

| Méthode | Endpoint | Auth | Description |
|---------|----------|------|-------------|
| `POST` | `/api/public/orders` | API Key | Créer une commande |
| `GET` | `/api/public/orders/{id}/status` | API Key | Statut d'une commande |

---

## 13. Déploiement et DevOps

### 13.1 Docker Compose (3 Services)

```yaml
Services:
  ┌─────────────────────────────────────────────────┐
  │  app (Spring Boot)                               │
  │  Port: 8080                                      │
  │  Image: Multi-stage build (3 étapes)             │
  │    1. node:20-alpine → Build React               │
  │    2. eclipse-temurin:17-jdk → Build Maven       │
  │    3. eclipse-temurin:17-jre → Runtime            │
  │  Volume: app-uploads                             │
  │  Dépend de: postgres, whatsapp-bridge            │
  ├─────────────────────────────────────────────────┤
  │  whatsapp-bridge (Node.js)                       │
  │  Port: 3100                                      │
  │  Image: node:18-bookworm-slim + Chromium         │
  │  Volume: whatsapp-bridge-auth                    │
  ├─────────────────────────────────────────────────┤
  │  postgres (PostgreSQL 16)                        │
  │  Port: 5433:5432 (hôte:conteneur)               │
  │  Volume: postgres-data                           │
  │  Health check: pg_isready                        │
  └─────────────────────────────────────────────────┘
```

### 13.2 Build Multi-Stage (Dockerfile Principal)

```
Étape 1 : frontend-builder (node:20-alpine)
    └── npm install → npm run build
    └── Produit: /app/dist/ (fichiers statiques)

Étape 2 : builder (eclipse-temurin:17-jdk)
    └── Copie le frontend dist vers src/main/resources/static/
    └── mvn package -DskipTests
    └── Produit: target/app.jar

Étape 3 : runtime (eclipse-temurin:17-jre)
    └── Utilisateur non-root (app:1001)
    └── Copie app.jar
    └── Expose port 8080
    └── CMD: java -jar app.jar
```

### 13.3 Volumes Persistants

| Volume | Service | Données |
|--------|---------|---------|
| `postgres-data` | postgres | Données PostgreSQL |
| `app-uploads` | app | Fichiers uploadés (Excel) |
| `whatsapp-bridge-auth` | whatsapp-bridge | Sessions WhatsApp authentifiées |

### 13.4 Nginx (Reverse Proxy Optionnel)

Configuration disponible dans `deploy/nginx/default.conf` pour un déploiement en production avec :
- Proxy vers le service `app` sur le port 8080
- Headers de forwarding (X-Real-IP, X-Forwarded-For, X-Forwarded-Proto)

---

## 14. Design Patterns et Bonnes Pratiques

### 14.1 Design Patterns Utilisés

| Pattern | Où | Description |
|---------|-----|-------------|
| **Repository Pattern** | `*Repository.java` | Abstraction de l'accès aux données via Spring Data JPA |
| **DTO Pattern** | `api/dtos/` | Séparation entre entités JPA et données API |
| **Mapper Pattern** | `api/mappers/` | Conversion explicite Entity ↔ DTO |
| **Service Layer** | `domain/*/service/` | Logique métier encapsulée dans les services |
| **Filter Chain** | `core/security/` | Chaîne de filtres pour l'authentification (API Key → JWT) |
| **Strategy Pattern** | `WhatsAppService` | Deux stratégies d'envoi : Twilio vs Bridge local |
| **Observer Pattern** | Webhooks | Notification des systèmes tiers lors d'événements |
| **Template Method** | Messages WhatsApp | Templates avec placeholders remplacés dynamiquement |
| **Builder Pattern** | `AuthResponse` (Lombok) | Construction fluide des objets de réponse |
| **Singleton** | Services Spring | Un seul instance par service (scope par défaut) |
| **Adapter Pattern** | `OzonExpressService` | Adaptation de l'API externe à l'interface interne |
| **Context Pattern** | React Context | `AuthContext`, `ThemeContext` pour l'état global |
| **Custom Hook** | `useStoreColor` | Encapsulation de logique réutilisable côté React |
| **Protected Route** | `ProtectedRoute` | Pattern de garde de navigation |

### 14.2 Principes de Clean Code

| Principe | Application |
|----------|-------------|
| **Séparation des préoccupations** | API / Domaine / Infrastructure clairement séparés |
| **DRY** | Services réutilisables, composants React modulaires |
| **Single Responsibility** | Chaque service/controller a un domaine précis |
| **Immutabilité** | DTOs comme objets de transfert immuables |
| **Validation centralisée** | Bean Validation + GlobalExceptionHandler |
| **Configuration externalisée** | application.properties, variables d'environnement Docker |

### 14.3 Bonnes Pratiques de Sécurité

| Pratique | Implémentation |
|----------|----------------|
| Hachage des mots de passe | BCrypt avec sel automatique |
| Tokens éphémères | Access token 1h, Refresh token 24h |
| Stateless | Pas de session serveur |
| CORS strict | Origines explicitement autorisées |
| Moindre privilège | RBAC avec 4 niveaux de rôles |
| Validation des entrées | Jakarta Validation sur tous les DTOs |

### 14.4 Bonnes Pratiques Frontend

| Pratique | Implémentation |
|----------|----------------|
| TypeScript strict | Typage statique sur tout le frontend |
| Composants fonctionnels | Hooks React (useState, useEffect, useContext) |
| Routing protégé | Guards de navigation avec redirection |
| Intercepteurs HTTP | Axios interceptors pour JWT et erreurs 401 |
| Responsive design | Tailwind CSS avec breakpoints mobile/tablette/desktop |
| Dark mode | Support complet via ThemeContext |
| Accessibilité | Composants sémantiques HTML5 |

---

## 15. Diagrammes

### 15.1 Diagramme de Cas d'Utilisation

```
                    ┌─────────────────────────────────────────┐
                    │            TALABATY                       │
                    │                                          │
  ┌──────────┐     │  ┌──────────────────────────────────┐   │
  │PLATFORM  │─────│─▶│ Gérer tous les comptes            │   │
  │  ADMIN   │     │  │ Gérer toutes les boutiques        │   │
  └──────────┘     │  │ Gérer les demandes de paiement    │   │
                    │  └──────────────────────────────────┘   │
  ┌──────────┐     │  ┌──────────────────────────────────┐   │
  │ ACCOUNT  │─────│─▶│ Créer/gérer ses boutiques         │   │
  │  OWNER   │     │  │ Gérer les utilisateurs            │   │
  │          │     │  │ Configurer les intégrations        │   │
  │          │     │  │ Voir le dashboard                  │   │
  └──────────┘     │  │ Configurer la livraison           │   │
                    │  │ Gérer les demandes de paiement    │   │
  ┌──────────┐     │  └──────────────────────────────────┘   │
  │ MANAGER  │─────│─▶│ Gérer ses boutiques assignées      │   │
  │          │     │  │ Gérer l'équipe de ses boutiques    │   │
  │          │     │  │ Configurer WhatsApp/Shipping       │   │
  │          │     │  │ Voir le dashboard                  │   │
  └──────────┘     │  └──────────────────────────────────┘   │
                    │  ┌──────────────────────────────────┐   │
  ┌──────────┐     │  │ Traiter les commandes (confirmation)│  │
  │ SUPPORT  │─────│─▶│ Voir ses commandes assignées      │   │
  │          │     │  │ Consulter ses gains                │   │
  │          │     │  │ Demander un paiement               │   │
  └──────────┘     │  └──────────────────────────────────┘   │
                    │                                          │
  ┌──────────┐     │  ┌──────────────────────────────────┐   │
  │ SYSTÈME  │─────│─▶│ Sync automatique (YouCan, GSheets)│   │
  │ EXTERNE  │     │  │ Créer commandes via API publique  │   │
  │          │     │  │ Recevoir webhooks                 │   │
  └──────────┘     │  └──────────────────────────────────┘   │
                    └─────────────────────────────────────────┘
```

### 15.2 Diagramme de Séquence - Création de Commande

```
Client          Frontend         Backend          Database        WhatsApp
  │                │                │                │                │
  │  Remplir form  │                │                │                │
  │───────────────▶│                │                │                │
  │                │ POST /orders   │                │                │
  │                │───────────────▶│                │                │
  │                │                │  Vérif. perms  │                │
  │                │                │───────────┐    │                │
  │                │                │◀──────────┘    │                │
  │                │                │                │                │
  │                │                │  Vérif. doublon │                │
  │                │                │───────────────▶│                │
  │                │                │◀───────────────│                │
  │                │                │                │                │
  │                │                │  Save order    │                │
  │                │                │───────────────▶│                │
  │                │                │◀───────────────│                │
  │                │                │                │                │
  │                │                │  Auto-distribute│               │
  │                │                │  (round-robin)  │               │
  │                │                │───────────────▶│                │
  │                │                │◀───────────────│                │
  │                │                │                │                │
  │                │  201 Created   │                │                │
  │                │◀───────────────│                │                │
  │  Afficher OK   │                │                │                │
  │◀───────────────│                │                │                │
```

### 15.3 Diagramme de Séquence - Confirmation + WhatsApp

```
Support         Frontend         Backend          Database      WhatsApp Bridge
  │                │                │                │                │
  │ Changer statut │                │                │                │
  │ → CONFIRMED    │                │                │                │
  │───────────────▶│                │                │                │
  │                │ PUT /status    │                │                │
  │                │───────────────▶│                │                │
  │                │                │                │                │
  │                │                │  Update status │                │
  │                │                │───────────────▶│                │
  │                │                │                │                │
  │                │                │  Save history  │                │
  │                │                │───────────────▶│                │
  │                │                │                │                │
  │                │                │  Credit support│                │
  │                │                │  revenue       │                │
  │                │                │───────────────▶│                │
  │                │                │                │                │
  │                │                │  Check WhatsApp│                │
  │                │                │  automation    │                │
  │                │                │───────────────▶│                │
  │                │                │                │                │
  │                │                │  Fill template │                │
  │                │                │  + Send msg    │                │
  │                │                │───────────────────────────────▶│
  │                │                │                │      POST /send│
  │                │                │◀───────────────────────────────│
  │                │                │                │                │
  │                │  200 OK        │                │                │
  │                │◀───────────────│                │                │
  │  ✅ Confirmé   │                │                │                │
  │◀───────────────│                │                │                │
```

### 15.4 Diagramme Entité-Relation Simplifié

```
┌────────────┐    1:N    ┌────────────┐    1:N    ┌────────────┐
│  accounts   │─────────▶│   users     │◀─────────│store_team  │
│             │          │             │          │  _members   │
│  id (PK)    │          │  id (PK)    │          │  id (PK)    │
│  name       │          │  email      │          │  role       │
│  type       │          │  role       │          │  status     │
└──────┬─────┘          └──────┬─────┘          └──────┬─────┘
       │                       │                        │
       │ 1:N                   │ N:1                    │ N:1
       ▼                       │                        │
┌────────────┐                │                ┌────────────┐
│   stores    │◀───────────────┘                │   orders    │
│             │────────────────────────────────▶│             │
│  id (PK)    │               1:N               │  id (PK)    │
│  name       │                                 │  source     │
│  code       │        ┌────────────┐           │  status     │
│  color      │   1:1  │store_      │           │  customer_* │
│  logo_url   │───────▶│ settings   │           │  total_amt  │
└──────┬─────┘        │            │           │  tracking   │
       │               │whatsapp_*  │           └──────┬─────┘
       │               │price_per_* │                  │
       │               └────────────┘                  │ 1:N
       │                                               ▼
       │ 1:N         ┌────────────┐           ┌────────────┐
       └────────────▶│ shipping   │           │order_status│
                     │ _providers │           │ _history   │
                     │            │           │            │
                     │provider_type│          │  status    │
                     │api_key     │           │  changed_by│
                     └────────────┘           └────────────┘
```

---

## 16. Conclusion

### 16.1 Résumé du Projet

**Talabaty** est une plateforme SaaS complète de gestion de commandes qui répond aux besoins spécifiques des e-commerçants marocains. Le projet démontre la maîtrise de :

| Domaine | Technologies et Concepts |
|---------|-------------------------|
| **Backend** | Java 17, Spring Boot 4.0, Spring Security, Spring Data JPA, API REST, JWT, RBAC |
| **Frontend** | React 18, TypeScript, Vite, Tailwind CSS, Chart.js, Three.js, WebGL |
| **Base de données** | PostgreSQL 16, Liquibase (migrations), JSONB, relations complexes |
| **Microservices** | Node.js, Express, whatsapp-web.js, architecture multi-services |
| **DevOps** | Docker, Docker Compose, multi-stage builds, Nginx |
| **Intégrations** | OAuth 2.0 (YouCan), Google Sheets API, Ozon Express API, WhatsApp |
| **Sécurité** | JWT, BCrypt, RBAC, API Key, CORS, validation |
| **Architecture** | DDD-Lite, Repository Pattern, DTO Pattern, Service Layer, Filter Chain |

### 16.2 Chiffres Clés

| Métrique | Valeur |
|----------|--------|
| Tables en base de données | 18 |
| Endpoints API REST | 60+ |
| Contrôleurs backend | 15 |
| Pages frontend | 17 |
| Composants React | 28 |
| Services frontend | 11 |
| Entités JPA | 12+ |
| Rôles utilisateur | 4 |
| Intégrations externes | 4 (YouCan, Google Sheets, Ozon Express, WhatsApp) |
| Services Docker | 3 |
| Villes marocaines mappées | 630+ |
| Transporteurs supportés | 11 |

### 16.3 Points Forts du Projet

1. **Architecture propre** : Séparation claire des couches (API, Domaine, Infrastructure)
2. **Multi-tenancy** : Isolation complète des données par compte et par boutique
3. **Sécurité robuste** : Double authentification (JWT + API Key), RBAC granulaire
4. **UX moderne** : Dark mode, thème dynamique par boutique, effets 3D, responsive
5. **Automatisation** : WhatsApp, synchronisation auto, distribution round-robin
6. **Extensibilité** : API publique, webhooks, architecture microservices
7. **Déploiement simplifié** : Docker Compose one-command deployment
8. **Adapté au marché** : Mapping 630+ villes marocaines, messages bilingues FR/AR

### 16.4 Perspectives d'Évolution

- Intégration de paiements en ligne (CMI, PayPal)
- Application mobile (React Native)
- Intégration de plus de transporteurs
- Intelligence artificielle pour la prédiction des annulations
- Système de notifications push
- Google OAuth pour l'authentification
- Support multi-langue complet (i18n)
- Tableau de bord plus avancé avec BI

---

*Documentation générée pour la soutenance de fin d'études - Projet Talabaty*
