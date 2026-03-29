import api from './api';
export interface SupportBalance {
    balance: number;
    totalEarned: number;
    totalPaid: number;
}
export interface PaymentRequestDto {
    id: string;
    amountRequested: number;
    status: string;
    requestedAt: string;
    processedAt?: string;
    note?: string;
    userId?: string;
    userName?: string;
    storeId?: string;
    storeName?: string;
    processedBy?: string;
}
export const supportService = {
    async getBalance(storeId: string): Promise<SupportBalance> {
        const { data } = await api.get<SupportBalance>(`/support/balance`, { params: { storeId } });
        return data;
    },
    async requestPayment(storeId: string, amount: number): Promise<PaymentRequestDto> {
        const { data } = await api.post<PaymentRequestDto>(`/support/request-payment`, { storeId, amount });
        return data;
    },
    async getMyPaymentRequests(storeId: string): Promise<PaymentRequestDto[]> {
        const { data } = await api.get<PaymentRequestDto[]>(`/support/payment-requests`, { params: { storeId } });
        return data;
    },
    async getAllPaymentRequests(): Promise<PaymentRequestDto[]> {
        const { data } = await api.get<PaymentRequestDto[]>(`/support/payment-requests/admin`);
        return data;
    },
    async markAsPaid(id: string, note?: string): Promise<PaymentRequestDto> {
        const { data } = await api.put<PaymentRequestDto>(`/support/payment-requests/${id}/paid`, { note });
        return data;
    },
    async rejectRequest(id: string, note?: string): Promise<PaymentRequestDto> {
        const { data } = await api.put<PaymentRequestDto>(`/support/payment-requests/${id}/rejected`, { note });
        return data;
    },
};
