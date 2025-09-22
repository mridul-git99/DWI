import * as React from 'react';

function SummarySkippedIcon(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg width="1em" height="1em" viewBox="0 0 24 24" fill="none" {...props}>
      <g clipPath="url(#prefix__clip0)">
        <path
          d="M19.977 1.942v7.75L7.991 2.77v4.615L0 2.77v18.462l7.99-4.616v4.616l11.987-6.924v7.75H24V1.942h-4.023z"
          fill="#C29004"
        />
      </g>
      <defs>
        <clipPath id="prefix__clip0">
          <path fill="#fff" d="M0 0h24v24H0z" />
        </clipPath>
      </defs>
    </svg>
  );
}

const MemoSummarySkippedIcon = React.memo(SummarySkippedIcon);
export default MemoSummarySkippedIcon;
