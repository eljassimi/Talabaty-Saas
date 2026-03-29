import { test, expect } from '@playwright/test';
const TEST_PASSWORD = 'TestPassword123';
function uniqueEmail(): string {
    return `e2e-${Date.now()}-${Math.random().toString(36).slice(2, 10)}@example.com`;
}
test.describe('Sign up', () => {
    test('sign up form shows and has required fields', async ({ page }) => {
        await page.goto('/signup');
        await expect(page.getByRole('heading', { name: /create an account/i })).toBeVisible();
        await expect(page.getByLabel(/first name/i)).toBeVisible();
        await expect(page.getByLabel(/last name/i)).toBeVisible();
        await expect(page.getByLabel(/your email/i)).toBeVisible();
        await expect(page.getByLabel(/^password$/i)).toBeVisible();
        await expect(page.getByLabel(/phone number/i)).toBeVisible();
        await expect(page.getByRole('button', { name: /get started/i })).toBeVisible();
    });
    test('full sign up flow redirects to app (select store or dashboard)', async ({ page }) => {
        const email = uniqueEmail();
        await page.goto('/signup');
        await page.getByLabel(/first name/i).fill('E2E');
        await page.getByLabel(/last name/i).fill('User');
        await page.getByLabel(/your email/i).fill(email);
        await page.getByLabel(/^password$/i).fill(TEST_PASSWORD);
        await page.getByLabel(/phone number/i).fill('+212600000000');
        await page.getByRole('button', { name: /get started/i }).click();
        await expect(page).not.toHaveURL(/\/signup/);
        await expect(page).not.toHaveURL(/\/login/);
        const url = page.url();
        const isSelectStore = url.includes('/select-store');
        const isChangePassword = url.includes('/change-password');
        const isDashboard = url === new URL('/', page.url()).href.replace(/\/?$/, '/') || url.endsWith('/');
        expect(isSelectStore || isChangePassword || isDashboard).toBeTruthy();
        if (isSelectStore) {
            await expect(page.getByRole('heading', { name: /select a store/i })).toBeVisible({ timeout: 10000 });
        }
    });
    test('link to sign in from sign up page works', async ({ page }) => {
        await page.goto('/signup');
        await page.getByRole('link', { name: /sign in/i }).click();
        await expect(page).toHaveURL(/\/login/);
        await expect(page.getByRole('heading', { name: /sign in to your account/i })).toBeVisible();
    });
});
test.describe('Sign in', () => {
    test('login form shows and has email and password', async ({ page }) => {
        await page.goto('/login');
        await expect(page.getByRole('heading', { name: /sign in to your account/i })).toBeVisible();
        await expect(page.getByLabel(/your email/i)).toBeVisible();
        await expect(page.getByLabel(/^password$/i)).toBeVisible();
        await expect(page.getByRole('button', { name: /get started/i })).toBeVisible();
    });
    test('sign in with wrong password shows error', async ({ page }) => {
        await page.goto('/login');
        await page.getByLabel(/your email/i).fill('wrong@example.com');
        await page.getByLabel(/^password$/i).fill('WrongPassword123');
        await page.getByRole('button', { name: /get started/i }).click();
        await expect(page.getByText(/invalid email or password/i)).toBeVisible({ timeout: 10000 });
        await expect(page).toHaveURL(/\/login/);
    });
    test('sign up then sign in with same user lands in app', async ({ page }) => {
        const email = uniqueEmail();
        await page.goto('/signup');
        await page.getByLabel(/first name/i).fill('E2E');
        await page.getByLabel(/last name/i).fill('Login');
        await page.getByLabel(/your email/i).fill(email);
        await page.getByLabel(/^password$/i).fill(TEST_PASSWORD);
        await page.getByLabel(/phone number/i).fill('+212600000001');
        await page.getByRole('button', { name: /get started/i }).click();
        await expect(page).not.toHaveURL(/\/signup/);
        await expect(page).not.toHaveURL(/\/login/);
        await page.context().clearCookies();
        await page.evaluate(() => {
            localStorage.clear();
        });
        await page.goto('/login');
        await page.getByLabel(/your email/i).fill(email);
        await page.getByLabel(/^password$/i).fill(TEST_PASSWORD);
        await page.getByRole('button', { name: /get started/i }).click();
        await expect(page).not.toHaveURL(/\/login/);
        const url = page.url();
        const isSelectStore = url.includes('/select-store');
        const isChangePassword = url.includes('/change-password');
        const isRoot = url.endsWith('/') || url.match(/localhost:3000\/?$/);
        expect(isSelectStore || isChangePassword || isRoot).toBeTruthy();
    });
    test('link to sign up from login page works', async ({ page }) => {
        await page.goto('/login');
        await page.getByRole('link', { name: /sign up/i }).click();
        await expect(page).toHaveURL(/\/signup/);
        await expect(page.getByRole('heading', { name: /create an account/i })).toBeVisible();
    });
});
