export function getSelectedStoreIdFromStorage(): string | null {
    try {
        const raw = localStorage.getItem('user');
        if (!raw)
            return null;
        const parsed = JSON.parse(raw) as {
            selectedStoreId?: string | null;
        };
        const id = parsed?.selectedStoreId;
        if (id == null || String(id).trim() === '')
            return null;
        return String(id).trim();
    }
    catch {
        return null;
    }
}
