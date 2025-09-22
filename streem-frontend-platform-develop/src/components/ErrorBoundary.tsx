import ErrorFallbackIcon from '#assets/svg/ErrorFallbackIcon';
import { Button } from '#components';
import { RootState } from '#store';
import { setGlobalError } from '#store/extras/action';
import React from 'react';
import { connect } from 'react-redux';
import styled from 'styled-components';
import * as Sentry from '@sentry/react';

const Wrapper = styled.div.attrs({
  className: 'wrapper',
})`
  align-items: center;
  display: flex;
  flex-direction: column;
  justify-content: center;

  .error-fallback {
    font-size: 300px;
  }

  h3 {
    color: #333333;
    font-size: 32px;
    line-height: 1.25;
    margin-bottom: 0;
    margin-top: 32px;
    text-align: center;
  }

  h6 {
    color: #666666;
    font-size: 14px;
    letter-spacing: 0.16px;
    line-height: 1.14;
    margin-bottom: 0;
    margin-top: 8px;
    text-align: center;
  }

  .reload {
    margin-top: 24px;
  }
`;

class ErrorBoundaryComponent extends React.Component<
  ReturnType<typeof mapStateToProps> & typeof mapDispatchToProps
> {
  constructor(props: any) {
    super(props);
  }

  componentDidCatch() {
    const { setGlobalError } = this.props;
    setGlobalError(true);
  }

  render() {
    const { children, hasGlobalError, setGlobalError, ...rest } = this.props;
    if (hasGlobalError) {
      return (
        <Wrapper {...rest}>
          <ErrorFallbackIcon className="icon error-fallback" />
          <h3>Oops! Looks like page did not load properly</h3>
          <h6>You can reload the page by clicking the button below</h6>
          <Button className="reload" onClick={() => setGlobalError(false)}>
            Reload Page
          </Button>
        </Wrapper>
      );
    }

    return <Sentry.ErrorBoundary>{children}</Sentry.ErrorBoundary>;
  }
}

const mapStateToProps = (state: RootState) => ({
  hasGlobalError: state.extras?.hasGlobalError,
});

const mapDispatchToProps = {
  setGlobalError,
};

const ErrorBoundary = connect(mapStateToProps, mapDispatchToProps)(ErrorBoundaryComponent);

export { ErrorBoundary };
