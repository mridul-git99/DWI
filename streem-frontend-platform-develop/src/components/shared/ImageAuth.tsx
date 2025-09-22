import { useTypedSelector } from '#store';
import React, { useEffect, useState } from 'react';
import { v4 as uuidv4 } from 'uuid';
import { LoadingContainer } from './LoadingContainer';
import { arrayBufferToBase64 } from '#utils/request';
import styled from 'styled-components';
import { GetAppOutlined, ErrorOutline, CheckCircleOutlined } from '@material-ui/icons';

const ImageWrapper = styled.div`
  display: flex;
  align-items: center;
  flex-direction: column;
  justify-content: center;
  height: 100%;
  width: 100%;

  .error-icon {
    color: #eb5757;
  }

  .download-success {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 8px;
    color: #161616;

    p {
      margin: 0;
      font-size: 20px;
      font-weight: bold;
    }
  }

  a {
    position: absolute;
    left: 16px;
    top: 24px;
    width: 24px;
    height: 24px;
    cursor: pointer;
    border-radius: 50%;
    background-color: rgba(0, 0, 0, 0.2);
    color: #fafafa;
  }
`;

export const ImageAuth = ({ id, src, alt, showDownload = false, ...rest }: any) => {
  const token = useTypedSelector((state) => state.auth.accessToken);
  const [link, setLink] = useState<any>();
  const [hasError, setHasError] = useState<boolean>(false);

  function fetchWithAuthentication(url: string) {
    const headers = new Headers();
    headers.set('Authorization', `Bearer ${token}`);
    return fetch(url, { headers });
  }

  async function displayProtectedImage(url: any) {
    const response = await fetchWithAuthentication(url);
    if (response.ok) {
      const contentType = response.headers.get('content-type');

      if (contentType && contentType.startsWith('image')) {
        const imageDataArrayBuffer = await response.arrayBuffer();
        const base64String = arrayBufferToBase64(imageDataArrayBuffer);
        const dataUrl = `data:${contentType};base64,${base64String}`;
        setLink(dataUrl);
      } else {
        const fileExtension = url.split('.').pop();
        const blob = await response.blob();
        const objectUrl = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = objectUrl;
        link.download = uuidv4() + `.${fileExtension}`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(objectUrl);
        setLink(true);
      }
    } else {
      setHasError(true);
      setLink(true);
    }
  }

  useEffect(() => {
    displayProtectedImage(src);
  }, [src]);

  return (
    <LoadingContainer
      loading={!link}
      style={{ width: '100%' }}
      component={
        <ImageWrapper>
          {hasError ? (
            <ErrorOutline className="error-icon" />
          ) : (
            <>
              {typeof link === 'string' ? (
                <>
                  <img src={link} alt={alt} {...rest} />
                  {showDownload && (
                    <a download={alt} href={link}>
                      <GetAppOutlined />
                    </a>
                  )}
                </>
              ) : (
                link && (
                  <div className="download-success">
                    <CheckCircleOutlined style={{ color: '#24A148' }} fontSize="large" />
                    <p>Your file has been downloaded!</p>
                    <span>You can now close this window.</span>
                  </div>
                )
              )}
            </>
          )}
        </ImageWrapper>
      }
    />
  );
};
