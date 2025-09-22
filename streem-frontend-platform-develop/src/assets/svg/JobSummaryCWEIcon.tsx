import * as React from 'react';

function SummaryCWEIcon(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg width="1em" height="1em" viewBox="0 0 24 24" fill="none" {...props}>
      <g clipPath="url(#prefix__clip0)" fill="#C29004">
        <path d="M23.297 24H6.422a.703.703 0 01-.703-.703v-7.031c0-.267.15-.51.388-.63L8.92 14.23a.704.704 0 01.629 0l2.498 1.25 2.498-1.25a.704.704 0 01.629 0l2.498 1.25 2.498-1.25a.704.704 0 01.629 0l2.812 1.407c.238.12.389.362.389.629v7.03a.703.703 0 01-.703.704zM14.063.412V4.22h3.806L14.063.412z" />
        <path d="M13.36 5.625a.703.703 0 01-.704-.703V0H.703A.703.703 0 000 .703V13.36a.703.703 0 001.018.63l2.498-1.25 2.498 1.25a.704.704 0 00.629 0l2.498-1.25 2.498 1.25a.704.704 0 00.629 0l2.498-1.25 2.498 1.25a.703.703 0 001.018-.629V5.624h-4.923zm-2.316 3.019a.703.703 0 11-.994.994l-.91-.91-.909.91a.703.703 0 11-.994-.994l.91-.91-.91-.909a.703.703 0 11.994-.994l.91.91.909-.91a.703.703 0 11.994.994l-.91.91.91.909z" />
      </g>
      <defs>
        <clipPath id="prefix__clip0">
          <path fill="#fff" d="M0 0h24v24H0z" />
        </clipPath>
      </defs>
    </svg>
  );
}

const MemoSummaryCWEIcon = React.memo(SummaryCWEIcon);
export default MemoSummaryCWEIcon;
