/**
 * @file index.ts
 * Main entry point for the Authentication Capacitor Plugin.
 *
 * This file registers the plugin with the Capacitor core runtime, wires the
 * provider-keyed facade with a per-provider single-flight `signIn` guard, and
 * re-exports the public type surface for consumers.
 */

import { registerPlugin } from '@capacitor/core';

import { AuthenticationPlugin, AuthProvider, SignInOptions, SocialAuthResult } from './definitions';

/**
 * The registered (web or native) plugin instance backing the facade.
 */
const instance = registerPlugin<AuthenticationPlugin>('Authentication', {
  web: () => import('./web').then((m) => new m.AuthenticationWeb()),
});

/**
 * Tracks the in-flight `signIn` promise per provider so concurrent calls for the
 * same provider share one flow instead of starting a second one.
 *
 * The key is removed as soon as the flow settles, allowing a fresh, sequential
 * sign-in afterwards.
 */
const inFlightSignIns = new Map<AuthProvider, Promise<SocialAuthResult>>();

/**
 * The public Authentication facade.
 *
 * `signIn` is wrapped with a single-flight guard (social-auth-facade spec);
 * every other method is delegated directly to the registered implementation.
 */
const Authentication = new Proxy(instance, {
  get(target: AuthenticationPlugin, prop: string | symbol, receiver: unknown): unknown {
    if (prop === 'signIn') {
      return (options: SignInOptions & { provider: AuthProvider }): Promise<SocialAuthResult> => {
        const provider = options.provider;
        const existing = inFlightSignIns.get(provider);
        if (existing) {
          return existing;
        }
        const pending = target.signIn(options).then(
          (result) => {
            inFlightSignIns.delete(provider);
            return result;
          },
          (error: unknown) => {
            inFlightSignIns.delete(provider);
            throw error;
          },
        );
        inFlightSignIns.set(provider, pending);
        return pending;
      };
    }
    return Reflect.get(target, prop, receiver);
  },
});

export * from './definitions';
export { Authentication };
