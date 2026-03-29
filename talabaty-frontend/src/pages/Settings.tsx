import { useState } from 'react';
import { User, Lock, CreditCard, Users, Store, Truck, ShoppingCart } from 'lucide-react';
import StoresSettings from '../components/settings/StoresSettings';
import ShippingProvidersSettings from '../components/settings/ShippingProvidersSettings';
import TeamSettings from '../components/settings/TeamSettings';
import LeadsSettings from '../components/settings/LeadsSettings';
import { useStoreColor } from '../hooks/useStoreColor';
type SettingsSection = 'profile' | 'security' | 'plan' | 'billing' | 'team' | 'stores' | 'delivery-companies' | 'leads';
export default function Settings() {
    const { storeColor } = useStoreColor();
    const [activeSection, setActiveSection] = useState<SettingsSection>('stores');
    const sections = [
        { id: 'profile' as SettingsSection, name: 'Profile', icon: User },
        { id: 'security' as SettingsSection, name: 'Security', icon: Lock },
        { id: 'plan' as SettingsSection, name: 'Plan', icon: CreditCard },
        { id: 'billing' as SettingsSection, name: 'Billing', icon: CreditCard },
        { id: 'team' as SettingsSection, name: 'Team', icon: Users },
        { id: 'leads' as SettingsSection, name: 'Leads', icon: ShoppingCart },
        { id: 'stores' as SettingsSection, name: 'Stores', icon: Store },
        { id: 'delivery-companies' as SettingsSection, name: 'Delivery Companies', icon: Truck },
    ];
    const renderContent = () => {
        switch (activeSection) {
            case 'stores':
                return <StoresSettings />;
            case 'delivery-companies':
                return <ShippingProvidersSettings />;
            case 'team':
                return <TeamSettings />;
            case 'leads':
                return <LeadsSettings />;
            case 'profile':
                return (<div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
            <h2 className="text-xl font-semibold text-gray-900 mb-4">Profile Settings</h2>
            <p className="text-gray-600">Profile settings coming soon...</p>
          </div>);
            case 'security':
                return (<div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
            <h2 className="text-xl font-semibold text-gray-900 mb-4">Security Settings</h2>
            <p className="text-gray-600">Security settings coming soon...</p>
          </div>);
            case 'plan':
                return (<div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
            <h2 className="text-xl font-semibold text-gray-900 mb-4">Plan Settings</h2>
            <p className="text-gray-600">Plan settings coming soon...</p>
          </div>);
            case 'billing':
                return (<div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
            <h2 className="text-xl font-semibold text-gray-900 mb-4">Billing Settings</h2>
            <p className="text-gray-600">Billing settings coming soon...</p>
          </div>);
            default:
                return null;
        }
    };
    return (<div className="space-y-6">
      
      <div>
        <h1 className="text-3xl font-bold text-gray-900">Settings</h1>
        <p className="mt-1 text-gray-600">Manage all your settings</p>
      </div>

      <div className="flex gap-6">
        
        <div className="w-64 flex-shrink-0">
          <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-4">
            <nav className="space-y-1">
              {sections.map((section) => {
            const Icon = section.icon;
            const isActive = activeSection === section.id;
            return (<button key={section.id} onClick={() => setActiveSection(section.id)} className={`w-full flex items-center px-4 py-3 text-sm font-medium rounded-lg transition-colors ${isActive
                    ? 'text-white'
                    : 'text-gray-700 hover:bg-gray-50'}`} style={isActive ? { backgroundColor: storeColor } : {}}>
                    <Icon className={`h-5 w-5 mr-3 ${isActive ? 'text-white' : 'text-gray-400'}`}/>
                    {section.name}
                  </button>);
        })}
            </nav>
          </div>
        </div>

        
        <div className="flex-1">{renderContent()}</div>
      </div>
    </div>);
}
