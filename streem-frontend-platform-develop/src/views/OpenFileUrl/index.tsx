import { ImageAuth } from '#components';
import { useQueryParams } from '#hooks/useQueryParams';
import React, { useState } from 'react';

const OpenFileUrl = () => {
  const { getQueryParam } = useQueryParams();
  const linkParam = getQueryParam('link');
  const [errorMessage, setErrorMessage] = useState('');

  const handleError = () => {
    setErrorMessage('Please Check the url and try again');
  };

  return (
    <>
      <ImageAuth src={linkParam} style={{ maxWidth: '100%' }} onError={handleError} />
      {errorMessage && (
        <div
          style={{
            width: '100vw',
            fontWeight: '600',
            fontSize: '20px',
            margin: '40px 0',
            display: 'flex',
            justifyContent: 'center',
          }}
        >
          {errorMessage}
        </div>
      )}
    </>
  );
};

export default OpenFileUrl;
