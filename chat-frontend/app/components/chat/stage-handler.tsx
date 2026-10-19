import { useState } from 'react';
import { toast } from 'sonner';
import type { ChatState } from '~/lib/chat/stateMachineReducer';
import type { AvailableUsername } from '~/lib/shared/types';
import { cn } from '~/lib/utils';

export function ChatViewHandler({
  state,
  onSendMessage,
}: {
  state: ChatState;
  onSendMessage: (message: string) => void;
}) {
  const [messageValue, setMessageValue] = useState('');

  const isBusy = state.phase !== 'IDLE';

  const handleSubmit = () => {
    const message = messageValue.trim();

    if (!message) {
      toast.error('El mensaje no puede estar vacío');
      return;
    }

    if (message.length > 1000) {
      toast.error('El mensaje no puede exceder los 1000 caracteres');
      return;
    }

    if (isBusy) {
      return;
    }

    onSendMessage(message);

    setMessageValue('');
  };

  return (
    <div className="border-muted mx-auto w-1/2 rounded-md border p-4">
      <div className="mb-4 space-y-2">
        {state.messages.map((message, index) => (
          <div key={message.id} className="rounded-md border p-3">
            {message.status === 'PENDING_VERIFICATION' ? (
              <div className="text-gray-500">
                Mensaje pendiente de verificar
              </div>
            ) : (
              <MessageBox
                username={state.username}
                messageContent={message.content}
              />
            )}
          </div>
        ))}
      </div>

      <div className="flex gap-2">
        <input
          name="content"
          type="text"
          value={messageValue}
          disabled={isBusy}
          className="flex-1 rounded-md border px-3 py-2"
          placeholder={
            isBusy ? 'Esperando respuesta...' : 'Escribe un mensaje...'
          }
          onChange={(event) => setMessageValue(event.currentTarget.value)}
          onKeyDown={(event) => {
            if (event.key === 'Enter') {
              handleSubmit();
            }
          }}
        />

        <button
          type="button"
          disabled={isBusy}
          className="rounded-md border px-4 py-2 disabled:opacity-50"
          onClick={handleSubmit}>
          Enviar
        </button>
      </div>
    </div>
  );
}

function MessageBox({
  username,
  messageContent,
}: {
  username: AvailableUsername;
  messageContent: string;
}) {
  return (
    <div className="grid">
      <div
        className={cn(
          'w-3/4',
          username == 'Fulanita' ? 'justify-self-end' : 'justify-self-start'
        )}>
        <span className="text-muted-foreground text-xs">
          Mensaje de {username}
        </span>

        <div>{messageContent}</div>
      </div>
    </div>
  );
}
