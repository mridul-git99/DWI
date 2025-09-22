import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';
import { request } from './request';

interface DownloadPdfOptions {
  url: string;
  method?: 'GET' | 'POST';
  params?: Record<string, any>;
  data?: Record<string, any>;
  headers?: Record<string, string>;
  filename?: string;
}

interface RequestOptionsWithResponseType {
  params?: Record<string, any>;
  data?: Record<string, any>;
  headers?: Record<string, string>;
  responseType?: 'blob' | 'json' | 'text' | 'arraybuffer';
}

export const downloadPdf = async ({
  url,
  method = 'GET',
  params,
  data,
  headers,
  filename = 'document.pdf',
}: DownloadPdfOptions) => {
  window.store.dispatch(
    showNotification({
      type: NotificationType.WARNING,
      msg: 'Generating PDF...',
    }),
  );
  try {
    const res = await request(method, url, {
      params,
      data,
      headers,
      responseType: 'blob',
    } as RequestOptionsWithResponseType);

    if (res) {
      const blob = new Blob([res], { type: 'application/pdf' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `${filename}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      window.store.dispatch(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: 'PDF downloaded successfully!',
        }),
      );
    } else {
      console.error('Empty response while downloading PDF.');
      window.store.dispatch(
        showNotification({
          type: NotificationType.ERROR,
          msg: 'PDF generation failed. Empty response received.',
        }),
      );
    }
  } catch (err) {
    console.error('PDF download failed:', err);
    window.store.dispatch(
      showNotification({
        type: NotificationType.ERROR,
        msg: 'PDF generation failed. Please try again.',
      }),
    );
  }
};
