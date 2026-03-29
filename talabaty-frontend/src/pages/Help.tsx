import { Mail, MessageCircle, FileText } from 'lucide-react';
import { useStoreColor } from '../hooks/useStoreColor';
export default function Help() {
    const { storeColor } = useStoreColor();
    return (<div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-[#111827]">Help & Support</h1>
        <p className="mt-1 text-sm text-[#6B7280]">
          Get help with Talabaty and manage your orders.
        </p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        <a href="mailto:support@talabaty.com" className="flex items-center gap-4 p-5 rounded-xl border border-[#E6E8EC] bg-white hover:shadow-md transition-shadow">
          <div className="h-12 w-12 rounded-xl bg-[#F6F8FB] flex items-center justify-center shrink-0" style={{ color: storeColor }}>
            <Mail className="h-6 w-6"/>
          </div>
          <div>
            <h2 className="text-sm font-semibold text-[#111827]">Email support</h2>
            <p className="text-xs text-[#6B7280]">support@talabaty.com</p>
          </div>
        </a>
        <a href="#" className="flex items-center gap-4 p-5 rounded-xl border border-[#E6E8EC] bg-white hover:shadow-md transition-shadow">
          <div className="h-12 w-12 rounded-xl bg-[#F6F8FB] flex items-center justify-center shrink-0" style={{ color: storeColor }}>
            <MessageCircle className="h-6 w-6"/>
          </div>
          <div>
            <h2 className="text-sm font-semibold text-[#111827]">Contact us</h2>
            <p className="text-xs text-[#6B7280]">Chat or send a message</p>
          </div>
        </a>
        <a href="#" className="flex items-center gap-4 p-5 rounded-xl border border-[#E6E8EC] bg-white hover:shadow-md transition-shadow sm:col-span-2">
          <div className="h-12 w-12 rounded-xl bg-[#F6F8FB] flex items-center justify-center shrink-0" style={{ color: storeColor }}>
            <FileText className="h-6 w-6"/>
          </div>
          <div>
            <h2 className="text-sm font-semibold text-[#111827]">Documentation</h2>
            <p className="text-xs text-[#6B7280]">Guides and FAQs</p>
          </div>
        </a>
      </div>
    </div>);
}
