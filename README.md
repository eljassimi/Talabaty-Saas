# Talabaty

Plateforme de **gestion de commandes et de livraison** pour e-commerce : plusieurs comptes, boutiques, équipes (rôles), intégration **YouCan**, envoi vers des transporteurs (**Ozon Express**), automatisations **WhatsApp**, API publique et webhooks. L’interface web et l’API REST sont servies par une même application Spring Boot en production.

---

## Fonctionnalités principales

- Authentification **JWT** (inscription, connexion, rafraîchissement de token)
- **Comptes** multi-tenant avec **boutiques** (codes, statuts, thème / couleur)
- **Commandes** : création, suivi, statuts, affectation aux membres support, envoi à la livraison
- **Intégration YouCan** (OAuth, synchronisation des commandes)
- **Fournisseurs de livraison** configurables par boutique
- **Équipe** par boutique (invitations, rôles)
- **Tableau de bord** et statistiques
- **Demandes de paiement** et suivi côté support (selon permissions)
- **Webhooks** et **clés API** pour intégrations externes
- Pont **WhatsApp** (service Node séparé) pour l’automatisation des messages

---

## Architecture du dépôt

| Dossier / fichier | Rôle |
|-------------------|------|
| `src/main/java/...` | Backend **Spring Boot** (API `/api`, sécurité, domaine) |
| `src/main/resources/` | Config Spring, **Liquibase** (migrations PostgreSQL) |
| `talabaty-frontend/` | SPA **React + TypeScript + Vite + Tailwind** |
| `whatsapp-bridge/` | Service Node pour WhatsApp (Puppeteer) |
| `Dockerfile` | Image unique : build frontend + JAR Spring Boot |
| `docker-compose.yml` | **App + PostgreSQL + WhatsApp bridge** |
| `docs/` | Documentation complémentaire (Docker, CI, backend, etc.) |

En **production Docker**, le frontend compilé est copié dans `classpath:/static` et servi avec l’API sur le **même port** (`8080`).

---

## Prérequis

- **Java 17**
- **Maven** (ou `./mvnw` à la racine)
- **Node.js 18+** et npm (développement frontend uniquement)
- **PostgreSQL 16** (ou utilisation de Docker Compose)

---

## Démarrage rapide avec Docker

À la racine du dépôt :

```bash
docker compose up --build
```

- Application (UI + API) : [http://localhost:8080](http://localhost:8080)
- PostgreSQL : port **5433** sur la machine hôte → conteneur `5432`
- Pont WhatsApp : port **3100**

Détails sur les images, variables d’environnement et CI : **[docs/DOCKER-AND-CICD.md](docs/DOCKER-AND-CICD.md)**.

---

## Développement local

### 1. Base de données

Créez une base PostgreSQL (ex. `talabaty`) et alignez `spring.datasource.*` dans `src/main/resources/application.properties` (ou utilisez un profil dédié).

Les schémas sont gérés par **Liquibase** (`spring.jpa.hibernate.ddl-auto=none`).

### 2. Backend

```bash
./mvnw spring-boot:run
```

L’API est exposée sur **http://localhost:8080** (dont le préfixe **`/api`** selon les contrôleurs).

### 3. Frontend (recommandé en dev)

```bash
cd talabaty-frontend
npm install
npm run dev
```

- UI : **http://localhost:3000**
- Le proxy Vite redirige **`/api`** vers **http://localhost:8080**

Pour un build seul du frontend : `npm run build` (voir aussi le Dockerfile pour l’intégration dans le JAR).

### 4. WhatsApp bridge (optionnel)

Si vous testez les fonctionnalités WhatsApp en local, lancez le service sous `whatsapp-bridge/` et renseignez `whatsapp.local.url` comme dans la configuration Spring.

---

## Tests

```bash
# Backend
./mvnw verify

# E2E frontend (Playwright)
cd talabaty-frontend && npm run e2e
```

---

## Configuration sensible

Ne commitez **pas** de secrets (mots de passe DB, `jwt.secret`, clés OAuth YouCan, etc.). Utilisez des variables d’environnement ou des fichiers locaux ignorés par Git pour vos déploiements.

---

## Documentation

- [Docker et CI GitHub Actions](docs/DOCKER-AND-CICD.md)
- Autres guides : répertoire [`docs/`](docs/)

---

## Licence

Non spécifiée dans ce dépôt ; précisez la licence souhaitée dans ce fichier et dans `pom.xml` si vous publiez le projet ouvertement.
