import * as React from 'react';

function SummaryErrorCorrectionIcon(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg width="1em" height="1em" viewBox="0 0 24 24" fill="none" {...props}>
      <g clipPath="url(#prefix__clip0)" fill="#C29004">
        <path d="M12 5.054a1.4 1.4 0 00-1.223.704l-5.046 8.707a1.399 1.399 0 00-.002 1.414c.255.443.713.708 1.224.708h10.094c.511 0 .969-.265 1.224-.708a1.399 1.399 0 00-.002-1.414l-5.046-8.707A1.4 1.4 0 0012 5.054zm0 9.219a.681.681 0 110-1.363.681.681 0 010 1.363zm.516-2.304h-1.032l-.258-2.478h1.548l-.258 2.478z" />
        <path d="M23.929 10.696l-1.399.155a10.626 10.626 0 01-1.577 6.811 10.61 10.61 0 01-5.59 4.384c-1.104.37-2.239.547-3.363.547-3.245 0-6.394-1.478-8.429-4.061h1.394v-1.408H1.202v3.936h1.407v-1.482a12.146 12.146 0 0013.202 3.802 12.021 12.021 0 006.332-4.967 12.038 12.038 0 001.786-7.717zM2.998 6.426a10.604 10.604 0 015.638-4.463 10.482 10.482 0 016.932.07A10.675 10.675 0 0120.1 5.18h-1.341v1.407h3.804v-3.76h-1.408V4.25A12.09 12.09 0 0016.043.71 11.876 11.876 0 008.19.629a12.014 12.014 0 00-6.388 5.057A12.026 12.026 0 00.098 13.53l1.395-.18a10.614 10.614 0 011.505-6.925z" />
      </g>
      <defs>
        <clipPath id="prefix__clip0">
          <path fill="#fff" d="M0 0h24v24H0z" />
        </clipPath>
      </defs>
    </svg>
  );
}

const MemoSummaryErrorCorrectionIcon = React.memo(SummaryErrorCorrectionIcon);
export default MemoSummaryErrorCorrectionIcon;
