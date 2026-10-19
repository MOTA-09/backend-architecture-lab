import { useEffect, useState } from 'react';
import { useActionData, useSubmit } from 'react-router';
import { toast } from 'sonner';
import { MessagingActionResponseEnum } from '~/lib/chat/response-types';
import type { ChatState } from '~/lib/chat/stateMachineReducer';
import type { AvailableUsername } from '~/lib/shared/types';
import {
  triggerMessageVerification,
  triggerSaveMessage,
} from '~/lib/various/form-submission/chat/action-triggers';
import type { action } from '~/routes/_index';
import { ChatViewHandler } from './stage-handler';

export function ChatController({ username }: { username: AvailableUsername }) {
  const actionData = useActionData<typeof action>();

  const submit = useSubmit();

  const [state, setState] = useState<ChatState>({
    username: username,
    phase: 'IDLE',
    draftMessage: '',
    activeMessageId: null,
    messages: [],
  });

  /*
   * --------------------------------------------------
   * SEND MESSAGE FUNCTION
   * --------------------------------------------------
   */
  const sendMessage = (message: string) => {
    if (state.phase !== 'IDLE') {
      return;
    }

    setState((current) => ({
      ...current,

      phase: 'CREATING',

      draftMessage: message,

      activeMessageId: null,
    }));
  };

  /*
   * --------------------------------------------------
   * CREATING
   *
   * React observes:
   *
   *     IDLE → CREATING
   *
   * and performs the POST.
   * --------------------------------------------------
   */
  useEffect(() => {
    if (state.phase !== 'CREATING') {
      return;
    }

    console.log('[CHAT] Creating message');
    console.log('[CHAT] Content:', state.draftMessage);

    triggerSaveMessage(
      {
        message: state.draftMessage,
      },
      submit
    );
  }, [state.phase, state.draftMessage, submit]);

  /*
   * --------------------------------------------------
   * BACKEND RESPONSE
   *
   * The action tells us what happened.
   *
   * MESSAGE_SAVED:
   *
   *     backend created the message
   *     and returned its ID.
   *
   * MESSAGE_VERIFIED:
   *
   *     backend found the message by ID.
   * --------------------------------------------------
   */
  useEffect(() => {
    if (actionData && actionData.success === false) {
      const errorMsg =
        actionData.error || 'El servidor rechazó el mensaje enviado';
      console.error('[CHAT] Server action returned error:', errorMsg);
      toast.error(errorMsg);

      setState((current) => ({
        ...current,
        phase: 'IDLE',
        activeMessageId: null,
      }));
      return;
    }

    if (!actionData?.success) {
      return;
    }
    /*
     * ----------------------------------------------
     * MESSAGE SAVED
     * ----------------------------------------------
     */
    if (
      actionData.performedAction ===
      MessagingActionResponseEnum.enum.MESSAGE_SAVED
    ) {
      const messageId = actionData.data.chatMessageId;

      console.log('[CHAT] MESSAGE_SAVED response:', actionData);

      console.log('[CHAT] Message ID:', messageId);

      if (!messageId) {
        console.error('[CHAT] Backend did not return a message ID');

        return;
      }

      setState((current) => ({
        ...current,

        phase: 'PENDING_VERIFICATION',

        activeMessageId: messageId,

        messages: [
          ...current.messages,

          {
            id: messageId,

            content: current.draftMessage,

            status: 'PENDING_VERIFICATION',
          },
        ],
      }));

      return;
    }
    /*
     * ----------------------------------------------
     * MESSAGE VERIFIED
     * ----------------------------------------------
     */
    if (
      actionData.performedAction ===
      MessagingActionResponseEnum.enum.MESSAGE_VERIFIED
    ) {
      const messageId = actionData.data.chatMessageId;

      const messageContent = actionData.data.messageContent;

      console.log('[CHAT] MESSAGE_VERIFIED response:', actionData);

      console.log('[CHAT] Message ID:', messageId);

      console.log('[CHAT] Message content:', messageContent);

      if (!messageId) {
        console.error(
          '[CHAT] Verification response did not contain a message ID'
        );

        return;
      }

      setState((current) => ({
        ...current,

        phase: 'VERIFIED',

        messages: current.messages.map((message) =>
          message.id === messageId
            ? {
                ...message,
                content: messageContent,
                status: 'VERIFIED',
              }
            : message
        ),
      }));
    }
  }, [actionData]);

  /*
   * --------------------------------------------------
   * PENDING_VERIFICATION
   *
   * Wait before performing the second request.
   * --------------------------------------------------
   */
  useEffect(() => {
    if (state.phase !== 'PENDING_VERIFICATION') {
      return;
    }

    if (!state.activeMessageId) {
      console.error('[CHAT] Cannot verify message: no active ID');

      return;
    }

    console.log('[CHAT] Message is pending:', state.activeMessageId);

    const timeout = setTimeout(() => {
      console.log('[CHAT] Verification delay finished');

      setState((current) => ({
        ...current,
        phase: 'VERIFYING',
      }));
    }, 1000);

    return () => {
      clearTimeout(timeout);
    };
  }, [state.phase, state.activeMessageId]);

  /*
   * --------------------------------------------------
   * VERIFYING
   *
   * Perform the second request:
   *
   *     "Does this ID still exist in the DB?"
   * --------------------------------------------------
   */
  useEffect(() => {
    if (state.phase !== 'VERIFYING') {
      return;
    }

    if (!state.activeMessageId) {
      console.error('[CHAT] Cannot verify message: no active ID');

      return;
    }

    console.log('[CHAT] Verifying message:', state.activeMessageId);

    triggerMessageVerification(
      {
        chatMessageId: state.activeMessageId,
      },
      submit
    );
  }, [state.phase, state.activeMessageId, submit]);

  /*
   * --------------------------------------------------
   * VERIFIED
   *
   * The message has completed its trip.
   *
   * The next message is now allowed.
   * --------------------------------------------------
   */
  useEffect(() => {
    if (state.phase !== 'VERIFIED') {
      return;
    }

    console.log('[CHAT] Message trip completed');

    setState((current) => ({
      ...current,

      phase: 'IDLE',

      draftMessage: '',

      activeMessageId: null,
    }));
  }, [state.phase]);

  return <ChatViewHandler state={state} onSendMessage={sendMessage} />;
}
