import z from 'zod';

/* Generics */
export const zod_string = z.string();

export const zod_uuid = z.string().uuid();
export const zod_nullish_uuid = z.string().uuid();

export const chatMessageId = zod_uuid;

export type AvailableUsername = 'Fulanito' | 'Fulanita';

export type ServerActionResult<P> = {
  success: boolean;
  performedAction?: P;
  data?: any;
  error?: string;
};
