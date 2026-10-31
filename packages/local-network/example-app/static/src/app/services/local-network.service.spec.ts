import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';

import { LocalNetworkService } from './local-network.service';

vi.mock('@capacitor/core', () => ({
  Capacitor: {
    getPlatform: () => 'android',
    isNativePlatform: () => true,
  },
}));

vi.mock('@cap-kit/local-network', () => ({
  LocalNetwork: {
    checkPermissions: vi.fn(),
    requestPermissions: vi.fn(),
    getPluginVersion: vi.fn(),
    addListener: vi.fn(),
    removeAllListeners: vi.fn(),
  },
}));

const { LocalNetwork } = await import('@cap-kit/local-network');

describe('LocalNetworkService', () => {
  let service: LocalNetworkService;
  let removeListener: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    vi.clearAllMocks();
    removeListener = vi.fn().mockResolvedValue(undefined);

    vi.mocked(LocalNetwork.checkPermissions).mockResolvedValue({ localNetwork: 'granted' });
    vi.mocked(LocalNetwork.requestPermissions).mockResolvedValue({ localNetwork: 'denied' });
    vi.mocked(LocalNetwork.getPluginVersion).mockResolvedValue({ version: '8.0.0' });
    vi.mocked(LocalNetwork.addListener).mockResolvedValue({ remove: removeListener });
    vi.mocked(LocalNetwork.removeAllListeners).mockResolvedValue(undefined);

    TestBed.configureTestingModule({});
    service = TestBed.inject(LocalNetworkService);
  });

  it('starts with an unknown permission state', () => {
    expect(service.permission()).toBe('unknown');
    expect(service.listening()).toBe(false);
    expect(service.events()).toEqual([]);
  });

  it('check() stores the permission state', async () => {
    await service.check();

    expect(LocalNetwork.checkPermissions).toHaveBeenCalledTimes(1);
    expect(service.permission()).toBe('granted');
    expect(service.error()).toBeUndefined();
  });

  it('request() stores the resulting state, including denials', async () => {
    await service.request();

    expect(LocalNetwork.requestPermissions).toHaveBeenCalledTimes(1);
    expect(service.permission()).toBe('denied');
  });

  it('loadVersion() stores the plugin version', async () => {
    await service.loadVersion();

    expect(service.version()).toBe('8.0.0');
  });

  it('reports plugin rejections as a readable error', async () => {
    vi.mocked(LocalNetwork.checkPermissions).mockRejectedValue({
      code: 'UNAVAILABLE',
      message: 'not available',
    });

    await service.check();

    expect(service.error()).toBe('UNAVAILABLE: not available');
  });

  it('startListening() registers one listener and logs received events', async () => {
    await service.startListening();

    expect(LocalNetwork.addListener).toHaveBeenCalledTimes(1);
    expect(service.listening()).toBe(true);

    const listener = vi.mocked(LocalNetwork.addListener).mock.calls[0][1];
    listener({ localNetwork: 'denied' });

    expect(service.permission()).toBe('denied');
    expect(service.events()).toHaveLength(1);
    expect(service.events()[0].state).toBe('denied');
  });

  it('startListening() is idempotent while a listener is active', async () => {
    await service.startListening();
    await service.startListening();

    expect(LocalNetwork.addListener).toHaveBeenCalledTimes(1);
  });

  it('stopListening() removes the handle and resets state', async () => {
    await service.startListening();
    await service.stopListening();

    expect(removeListener).toHaveBeenCalledTimes(1);
    expect(service.listening()).toBe(false);
  });

  it('removeAllListeners() delegates to the plugin and resets state', async () => {
    await service.startListening();
    await service.removeAllListeners();

    expect(LocalNetwork.removeAllListeners).toHaveBeenCalledTimes(1);
    expect(service.listening()).toBe(false);
  });

  it('clearEvents() empties the log', async () => {
    await service.startListening();
    vi.mocked(LocalNetwork.addListener).mock.calls[0][1]({ localNetwork: 'prompt' });

    service.clearEvents();

    expect(service.events()).toEqual([]);
  });
});
