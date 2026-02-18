import { WebPlugin } from '@capacitor/core';

import {
  PeoplePlugin,
  PeoplePluginPermissions,
  PeopleCapabilities,
  GetContactsResult,
  UnifiedContact,
  PeopleProjection,
  PluginVersionResult,
  ListGroupsResult,
  CreateGroupOptions,
  CreateGroupResult,
  DeleteGroupOptions,
  AddPeopleToGroupOptions,
  RemovePeopleFromGroupOptions,
  CreateContactOptions,
  CreateContactResult,
  UpdateContactOptions,
  UpdateContactResult,
  DeleteContactOptions,
  MergeContactsOptions,
  MergeContactsResult,
} from './definitions';

type WebContact = {
  name?: string[];
  tel?: string[];
  email?: string[];
};

type ContactsNavigator = Navigator & {
  contacts?: {
    select: (props: string[], options: { multiple: boolean }) => Promise<WebContact[]>;
  };
};

/**
 * Class representing the web implementation of the PeoplePlugin.
 * This class extends the WebPlugin class and implements the PeoplePlugin interface.
 * It provides a base implementation for web-based functionality of the plugin.
 */
export class PeopleWeb extends WebPlugin implements PeoplePlugin {
  constructor() {
    super();
  }

  // -----------------------------------------------------------------------------
  // Capabilities
  // -----------------------------------------------------------------------------

  /**
   * Retrieves the capabilities of the People plugin on the web platform.
   *
   * @returns A promise resolving to the PeopleCapabilities object.
   */
  async getCapabilities(): Promise<PeopleCapabilities> {
    return {
      canRead: false, // Web cannot bulk read the address book
      canWrite: false,
      canObserve: false,
      canManageGroups: false,
      canPickContact: true, // Only the Zero-Permission picker is supported via Contact Picker API
    };
  }

  // -----------------------------------------------------------------------------
  // Permissions
  // -----------------------------------------------------------------------------

  /**
   * Checks the permission status for the plugin.
   *
   * @returns A promise resolving to an object containing the permission states.
   */
  async checkPermissions(): Promise<PeoplePluginPermissions> {
    return { contacts: 'prompt' };
  }

  /**
   * Requests the necessary permissions for the plugin.
   *
   * @returns A promise resolving to an object containing the updated permission states.
   */
  async requestPermissions(): Promise<PeoplePluginPermissions> {
    return { contacts: 'prompt' };
  }

  // -----------------------------------------------------------------------------
  // Contact Picking
  // -----------------------------------------------------------------------------

  /**
   * Launches the OS contact picker UI.
   * On Web this uses the Contact Picker API if available.
   * * Architectural rules:
   * - Rejects with CANCELLED on user cancellation to match native behavior.
   */
  async pickContact(options?: { projection?: PeopleProjection[] }): Promise<{ contact: UnifiedContact }> {
    const contactsNavigator = navigator as ContactsNavigator;
    const props = options?.projection || ['name', 'phones', 'emails'];
    const supportedProjection = new Set<PeopleProjection>([
      'name',
      'organization',
      'birthday',
      'phones',
      'emails',
      'addresses',
      'urls',
      'note',
    ]);

    for (const field of props) {
      if (!supportedProjection.has(field)) {
        return Promise.reject({
          message: `Unsupported projection field: ${field}`,
          code: 'UNKNOWN_TYPE',
        });
      }
    }

    // Support for the modern Web Contact Picker API
    if (contactsNavigator.contacts?.select) {
      try {
        // Marshalling: Map plugin projections to Web API properties
        const webProps = props.map((p) => {
          if (p === 'phones') return 'tel';
          if (p === 'emails') return 'email';
          return p;
        });

        const contacts = await contactsNavigator.contacts.select(webProps, { multiple: false });

        // Some browsers return an empty array instead of throwing on cancel; normalize to CANCELLED.
        if (!contacts || contacts.length === 0) {
          return Promise.reject({
            message: 'User cancelled selection',
            code: 'CANCELLED',
          });
        }

        const raw = contacts[0];

        return {
          contact: {
            id: 'web-ref',
            name: { display: raw.name?.[0] || 'Unknown' },
            phones: raw.tel?.map((t: string) => ({ number: t })) || [],
            emails: raw.email?.map((e: string) => ({ address: e })) || [],
          },
        };
      } catch (e: unknown) {
        const err = e as { name?: string; message?: string };
        // Map specific Web API cancellation to the standardized CANCELLED code
        if (err.name === 'AbortError') {
          return Promise.reject({
            message: 'User cancelled selection',
            code: 'CANCELLED',
          });
        }
        throw this.unavailable(err.message || 'Web Contact Picker API failed');
      }
    }

    throw this.unavailable('Native Contact Picker not available in this browser.');
  }

  // -----------------------------------------------------------------------------
  // Directory Access (Not supported on Web)
  // -----------------------------------------------------------------------------

  /**
   * Retrieves contacts from the system directory.
   * @param _options - Options for retrieving contacts.
   *
   * @returns A promise resolving to the contacts result.
   */
  async getContacts(): Promise<GetContactsResult> {
    throw this.unimplemented('getContacts is not available on Web.');
  }

  /**
   * Retrieves a single contact by ID.
   * @param _options - Options containing the contact ID.
   *
   * @returns A promise resolving to the contact.
   */
  async getContact(): Promise<{ contact: UnifiedContact }> {
    throw this.unimplemented('getContact is not available on Web.');
  }

  /**
   * Searches contacts in the system directory.
   * @param _options - Options for searching contacts.
   *
   * @returns A promise resolving to the contacts result.
   */
  async searchPeople(): Promise<GetContactsResult> {
    throw this.unimplemented('searchPeople is not available on Web.');
  }

  // -----------------------------------------------------------------------------
  // Group Management (Not supported on Web)
  // -----------------------------------------------------------------------------

  /**
   * Lists all available contact groups.
   * @returns A promise that is rejected because this feature is not available on Web.
   */
  async listGroups(): Promise<ListGroupsResult> {
    throw this.unimplemented('listGroups is not available on Web.');
  }

  /**
   * Creates a new contact group.
   * @returns A promise that is rejected because this feature is not available on Web.
   */
  async createGroup(_options: CreateGroupOptions): Promise<CreateGroupResult> {
    void _options;
    throw this.unimplemented('createGroup is not available on Web.');
  }

  /**
   * Deletes a contact group.
   * @returns A promise that is rejected because this feature is not available on Web.
   */
  async deleteGroup(_options: DeleteGroupOptions): Promise<void> {
    void _options;
    throw this.unimplemented('deleteGroup is not available on Web.');
  }

  /**
   * Adds contacts to a group.
   * @returns A promise that is rejected because this feature is not available on Web.
   */
  async addPeopleToGroup(_options: AddPeopleToGroupOptions): Promise<void> {
    void _options;
    throw this.unimplemented('addPeopleToGroup is not available on Web.');
  }

  /**
   * Removes contacts from a group.
   * @returns A promise that is rejected because this feature is not available on Web.
   */
  async removePeopleFromGroup(_options: RemovePeopleFromGroupOptions): Promise<void> {
    void _options;
    throw this.unimplemented('removePeopleFromGroup is not available on Web.');
  }

  // -----------------------------------------------------------------------------
  // CRUD (Not supported on Web)
  // -----------------------------------------------------------------------------

  /**
   * Creates a new contact.
   * @returns A promise that is rejected because this feature is not available on Web.
   */
  async createContact(_options: CreateContactOptions): Promise<CreateContactResult> {
    void _options;
    throw this.unimplemented('createContact is not available on Web.');
  }

  /**
   * Updates an existing contact.
   * @returns A promise that is rejected because this feature is not available on Web.
   */
  async updateContact(_options: UpdateContactOptions): Promise<UpdateContactResult> {
    void _options;
    throw this.unimplemented('updateContact is not available on Web.');
  }

  /**
   * Deletes a contact.
   * @returns A promise that is rejected because this feature is not available on Web.
   */
  async deleteContact(_options: DeleteContactOptions): Promise<void> {
    void _options;
    throw this.unimplemented('deleteContact is not available on Web.');
  }

  /**
   * Merges two contacts.
   * @returns A promise that is rejected because this feature is not available on Web.
   */
  async mergeContacts(_options: MergeContactsOptions): Promise<MergeContactsResult> {
    void _options;
    throw this.unimplemented('mergeContacts is not available on Web.');
  }

  // -----------------------------------------------------------------------------
  // Plugin Info
  // -----------------------------------------------------------------------------

  /**
   * Returns the plugin version.
   *
   * On the Web, this value represents the JavaScript package version
   * rather than a native implementation.
   */
  async getPluginVersion(): Promise<PluginVersionResult> {
    return { version: 'web' };
  }

  // -----------------------------------------------------------------------------
  // Listener Handling
  // -----------------------------------------------------------------------------

  /**
   * Cleanup all listeners
   *
   * @returns A promise that resolves when all listeners are removed.
   */
  async removeAllListeners(): Promise<void> {
    super.removeAllListeners();
  }
}
