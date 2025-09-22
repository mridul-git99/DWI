import { ParameterProps } from '#PrototypeComposer/Activity/types';
import { ContentState, EditorState } from 'draft-js';
import htmlToDraft from 'html-to-draftjs';
import React, { FC, useEffect, useState } from 'react';
import { Editor } from 'react-draft-wysiwyg';
import 'react-draft-wysiwyg/dist/react-draft-wysiwyg.css';
import styled from 'styled-components';

export const Wrapper = styled.div`
  .editor-class {
    border: none;
    padding: 0;
    pointer-events: none;
    overflow-wrap: break-word;

    * {
      font-weight: unset;
    }

    .public-* {
      margin: 0;
    }
  }

  .toolbar-class {
    display: none;
  }
`;

const TextInstructionTaskView: FC<Pick<ParameterProps, 'parameter'>> = ({ parameter }) => {
  const [editorState, setEditorState] = useState<EditorState | null>(null);

  useEffect(() => {
    const contentBlock = htmlToDraft(parameter.data.text || '');
    if (contentBlock) {
      const contentState = ContentState.createFromBlockArray(contentBlock.contentBlocks);
      setEditorState(EditorState.createWithContent(contentState));
    } else {
      setEditorState(EditorState.createEmpty());
    }
  }, [parameter.data.text]);

  if (!editorState) return null;

  return (
    <Wrapper>
      <Editor
        editorState={editorState}
        wrapperClassName="wrapper-class"
        editorClassName="editor-class"
        toolbarClassName="toolbar-class"
      />
    </Wrapper>
  );
};

export default TextInstructionTaskView;
