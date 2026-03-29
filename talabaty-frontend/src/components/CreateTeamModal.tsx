import { useState } from 'react';
import { teamService, TeamMemberRequest } from '../services/teamService';
import { X, Plus, Trash2, Users } from 'lucide-react';
interface CreateTeamModalProps {
    storeId: string;
    onClose: () => void;
    onSuccess: () => void;
}
export default function CreateTeamModal({ storeId, onClose, onSuccess }: CreateTeamModalProps) {
    const [managers, setManagers] = useState<TeamMemberRequest[]>([
        { email: '', firstName: '', lastName: '', password: '' },
    ]);
    const [supports, setSupports] = useState<TeamMemberRequest[]>([
        { email: '', firstName: '', lastName: '', password: '' },
    ]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const addManager = () => {
        setManagers([...managers, { email: '', firstName: '', lastName: '', password: '' }]);
    };
    const removeManager = (index: number) => {
        if (managers.length > 1) {
            setManagers(managers.filter((_, i) => i !== index));
        }
    };
    const updateManager = (index: number, field: keyof TeamMemberRequest, value: string) => {
        const updated = [...managers];
        updated[index] = { ...updated[index], [field]: value };
        setManagers(updated);
    };
    const addSupport = () => {
        setSupports([...supports, { email: '', firstName: '', lastName: '', password: '' }]);
    };
    const removeSupport = (index: number) => {
        if (supports.length > 1) {
            setSupports(supports.filter((_, i) => i !== index));
        }
    };
    const updateSupport = (index: number, field: keyof TeamMemberRequest, value: string) => {
        const updated = [...supports];
        updated[index] = { ...updated[index], [field]: value };
        setSupports(updated);
    };
    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError(null);
        setLoading(true);
        try {
            const validManagers = managers.filter((m) => m.email && m.firstName && m.lastName);
            const validSupports = supports.filter((s) => s.email && s.firstName && s.lastName);
            if (validManagers.length === 0 && validSupports.length === 0) {
                setError('Please add at least one manager or support member');
                setLoading(false);
                return;
            }
            await teamService.bulkCreateTeamMembers(storeId, {
                managers: validManagers,
                supports: validSupports,
            });
            onSuccess();
        }
        catch (err: any) {
            setError(err.response?.data?.error || 'Failed to create team members');
        }
        finally {
            setLoading(false);
        }
    };
    return (<div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
      <div className="relative top-20 mx-auto p-5 border w-full max-w-4xl shadow-lg rounded-md bg-white">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-medium text-gray-900 flex items-center">
            <Users className="h-5 w-5 mr-2"/>
            Create Team
          </h3>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600">
            <X className="h-6 w-6"/>
          </button>
        </div>

        {error && (<div className="mb-4 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
            {error}
          </div>)}

        <form onSubmit={handleSubmit} className="space-y-6">
          
          <div>
            <div className="flex items-center justify-between mb-3">
              <h4 className="text-md font-medium text-gray-900">Managers</h4>
              <button type="button" onClick={addManager} className="inline-flex items-center px-3 py-1.5 border border-transparent text-sm font-medium rounded-md text-white bg-primary-600 hover:bg-primary-700">
                <Plus className="h-4 w-4 mr-1"/>
                Add Manager
              </button>
            </div>
            <div className="space-y-3">
              {managers.map((manager, index) => (<div key={index} className="grid grid-cols-12 gap-3 items-end">
                  <div className="col-span-3">
                    <label className="block text-sm font-medium text-gray-700">
                      First Name
                    </label>
                    <input type="text" required value={manager.firstName} onChange={(e) => updateManager(index, 'firstName', e.target.value)} className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-primary-500 focus:ring-primary-500 sm:text-sm"/>
                  </div>
                  <div className="col-span-3">
                    <label className="block text-sm font-medium text-gray-700">
                      Last Name
                    </label>
                    <input type="text" required value={manager.lastName} onChange={(e) => updateManager(index, 'lastName', e.target.value)} className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-primary-500 focus:ring-primary-500 sm:text-sm"/>
                  </div>
                  <div className="col-span-4">
                    <label className="block text-sm font-medium text-gray-700">
                      Email
                    </label>
                    <input type="email" required value={manager.email} onChange={(e) => updateManager(index, 'email', e.target.value)} className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-primary-500 focus:ring-primary-500 sm:text-sm"/>
                  </div>
                  <div className="col-span-1">
                    <label className="block text-sm font-medium text-gray-700">
                      Password (Optional)
                    </label>
                    <input type="password" value={manager.password || ''} onChange={(e) => updateManager(index, 'password', e.target.value)} placeholder="Auto" className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-primary-500 focus:ring-primary-500 sm:text-sm"/>
                  </div>
                  <div className="col-span-1">
                    {managers.length > 1 && (<button type="button" onClick={() => removeManager(index)} className="text-red-600 hover:text-red-800">
                        <Trash2 className="h-5 w-5"/>
                      </button>)}
                  </div>
                </div>))}
            </div>
          </div>

          
          <div>
            <div className="flex items-center justify-between mb-3">
              <h4 className="text-md font-medium text-gray-900">Supports (Confirmation)</h4>
              <button type="button" onClick={addSupport} className="inline-flex items-center px-3 py-1.5 border border-transparent text-sm font-medium rounded-md text-white bg-primary-600 hover:bg-primary-700">
                <Plus className="h-4 w-4 mr-1"/>
                Add Support
              </button>
            </div>
            <div className="space-y-3">
              {supports.map((support, index) => (<div key={index} className="grid grid-cols-12 gap-3 items-end">
                  <div className="col-span-3">
                    <label className="block text-sm font-medium text-gray-700">
                      First Name
                    </label>
                    <input type="text" required value={support.firstName} onChange={(e) => updateSupport(index, 'firstName', e.target.value)} className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-primary-500 focus:ring-primary-500 sm:text-sm"/>
                  </div>
                  <div className="col-span-3">
                    <label className="block text-sm font-medium text-gray-700">
                      Last Name
                    </label>
                    <input type="text" required value={support.lastName} onChange={(e) => updateSupport(index, 'lastName', e.target.value)} className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-primary-500 focus:ring-primary-500 sm:text-sm"/>
                  </div>
                  <div className="col-span-4">
                    <label className="block text-sm font-medium text-gray-700">
                      Email
                    </label>
                    <input type="email" required value={support.email} onChange={(e) => updateSupport(index, 'email', e.target.value)} className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-primary-500 focus:ring-primary-500 sm:text-sm"/>
                  </div>
                  <div className="col-span-1">
                    <label className="block text-sm font-medium text-gray-700">
                      Password (Optional)
                    </label>
                    <input type="password" value={support.password || ''} onChange={(e) => updateSupport(index, 'password', e.target.value)} placeholder="Auto" className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-primary-500 focus:ring-primary-500 sm:text-sm"/>
                  </div>
                  <div className="col-span-1">
                    {supports.length > 1 && (<button type="button" onClick={() => removeSupport(index)} className="text-red-600 hover:text-red-800">
                        <Trash2 className="h-5 w-5"/>
                      </button>)}
                  </div>
                </div>))}
            </div>
          </div>

          <div className="flex items-center justify-end space-x-3 pt-4 border-t">
            <button type="button" onClick={onClose} className="px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50">
              Cancel
            </button>
            <button type="submit" disabled={loading} className="px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-primary-600 hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500 disabled:opacity-50">
              {loading ? 'Creating...' : 'Create Team'}
            </button>
          </div>
        </form>
      </div>
    </div>);
}
