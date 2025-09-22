import React from 'react';

function DownloadIcon(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg width="1em" height="1em" fill="none" viewBox="0 0 20 20" {...props}>
      <path
        fill="#000"
        fillRule="evenodd"
        clipRule="evenodd"
        d="M16.25 9.375L15.3687 8.49375L10.625 13.2312V1.25H9.375V13.2312L4.63125 8.49375L3.75 9.375L10 15.625L16.25 9.375ZM16.25 15V17.5H3.75V15H2.5V17.5C2.5 18.1904 3.05964 18.75 3.75 18.75H16.25C16.9404 18.75 17.5 18.1904 17.5 17.5V15H16.25Z"
      />
    </svg>
  );
}

export default DownloadIcon;
