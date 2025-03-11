// 创建支付弹窗
function createPaymentModal(contentId, price) {
    const modal = document.createElement('div');
    modal.className = 'payment-modal';
    modal.id = `payment-modal-${contentId}`;
    
    modal.innerHTML = `
        <div class="payment-modal-content">
<!--            <button class="payment-close" onclick="closePaymentModal('${contentId}')">&times;</button>-->
            <div class="payment-title">购买付费内容</div>
            <div class="payment-amount">¥${price}</div>
            <div class="payment-qrcode" id="qrcode-${contentId}">
                <img src="" alt="支付二维码" id="qrcode-img-${contentId}">
            </div>
            <div class="payment-status" id="payment-status-${contentId}">正在等待支付...</div>
            <div class="payment-countdown" id="countdown-${contentId}">支付倒计时：<span>05:00</span></div>
            <div class="payment-tips">请不要随意更改支付金额！尽快支付，支付成功后会有5至10秒的延迟，请耐心等待，支付过程中请不要刷新窗口！</div>
        </div>
    `;
    
    document.body.appendChild(modal);
    return modal;
}

// 关闭支付弹窗
function closePaymentModal(contentId) {

    const modalEl = document.getElementById(`payment-modal-${contentId}`);
    if (modalEl) {
        // 清除倒计时
        const timerId = modalEl.dataset.timerId;
        if (timerId) {
            console.log('清除定时器:', timerId); // 调试日志
            clearInterval(Number(timerId));
        }
                
        // 清除轮询定时器
        const pollIntervalId = modalEl.dataset.pollIntervalId;
        if (pollIntervalId) {
            clearInterval(Number(pollIntervalId));
        }
        modalEl.style.display = 'none';
    }
}

// 更新支付状态显示
function updatePaymentStatus(contentId, message) {
    const statusElement = document.getElementById(`payment-status-${contentId}`);
    if (statusElement) {
        statusElement.textContent = message;
    }
}

// 处理购买操作
async function handlePurchase(contentId) {
    try {
        const button = document.querySelector(`button[data-content-id="${contentId}"]`);
        const price = button.getAttribute('data-price');
        // 创建支付弹窗
        const modal = createPaymentModal(contentId, price);
        const clientAndContentString =  JSON.stringify({
            clientId: getClientId(),
            contentId: contentId
        })

        // const url = `/apis/plugin-paywall.halo.run/v1alpha1/paywall/purchase/${clientAndContentString}`;
        //
        //
        // console.log('url:', url);
        // const response = await fetch(url, {
        //     method: 'GET',
        //     headers: {
        //         'Content-Type': 'application/json'
        //     }
        // });
        const response = await fetch(`/apis/plugin-paywall.halo.run/v1alpha1/paywall/purchase/${clientAndContentString}`);
        
        if (!response.ok) {
            throw new Error('创建订单失败');
        }
        
        const data = await response.json();
        console.log('data:', data);
        
        // 显示二维码
        const qrcodeImg = document.getElementById(`qrcode-img-${contentId}`);
        if (qrcodeImg) {
            qrcodeImg.src = "data:image/png;base64,"+data.qrCodeUrl;
        }
        
        // 开始检查支付状态
        await checkPaymentStatus(contentId, data.orderId);

        // 启动倒计时
        if (data.expireTime) {
            startCountdown(contentId, data.expireTime);
        }



    } catch (error) {
        console.error('处理支付失败:', error);
        updatePaymentStatus(contentId, '创建支付订单失败，请重试');

    }
}


// 开始轮询支付状态
function checkPaymentStatus(contentId,orderId) {

    const pollInterval = setInterval(async () => {

        try {
            const statusResponse = await fetch(`/apis/plugin-paywall.halo.run/v1alpha1/paywall/status/${orderId}`);
            if (!statusResponse.ok) {
                throw new Error('检查支付状态失败');
            }

            const statusData = await statusResponse.json();

            const statusEl = document.getElementById(`payment-status-${contentId}`);
            // 更新状态显示
            if (statusEl) {
                statusEl.textContent = statusData.message || '正在等待支付...';
            }

            // 处理不同的支付状态
            switch (statusData.payStatus) {
                case 'SUCCESS':
                    clearInterval(pollInterval);
                    fetchContent(contentId, orderId);
                    closePaymentModal(contentId);
                    break;
                case 'EXPIRED':
                    clearInterval(pollInterval);
                    statusEl.textContent = '支付已超时';
                    closePaymentModal(contentId);
                    break;
                case 'PENDING':
                    //继续等待，更新倒计时
                    if (statusData.expireTime) {
                        updateCountdown(contentId, statusData.expireTime);
                    }
                    break;
            }
        } catch (error) {
            console.error('检查支付状态失败:', error);
        }
    }, 3000);

    // 保存轮询定时器ID
    const modalEl = document.getElementById(`payment-modal-${contentId}`);
    if (modalEl) {
        modalEl.dataset.pollIntervalId = pollInterval.toString();
        console.log('保存轮询定时器ID:', pollInterval); // 调试日志
    }

    // 启动倒计时
    // if (statusData.expireTime) {
    //     startCountdown(contentId, statusData.expireTime);
    // }

};


// 获取付费内容
function fetchContent(contentId,orderId) {
    fetch(`/apis/plugin-paywall.halo.run/v1alpha1/paywall/content/${orderId}`)
        .then(response => response.json())
        .then(data => {
            if (data.payStatus === 'SUCCESS') {
                // 更新内容
                const container = document.querySelector(`#content-${contentId}`);
                container.innerHTML = data.content;
                container.style.display = 'block';
                
                // 隐藏支付区域
                const paymentArea = container.parentElement.querySelector('.paywall-payment-area');
                paymentArea.style.display = 'none';
                
                // 隐藏预览区域
                const previewArea = container.parentElement.querySelector('.paywall-preview');
                previewArea.style.display = 'none';
            }
        })
        .catch(error => {
            console.error('获取内容失败:', error);
            alert('获取内容失败，请刷新页面重试');
        });
}


// 生成或获取客户端ID
function getClientId() {
    let clientId = localStorage.getItem('paywall_client_id');
    if (!clientId) {
        // 优先使用 crypto.randomUUID()
        if (crypto.randomUUID) {
            clientId = crypto.randomUUID();
        } else {
            // 后备方案
            clientId = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
                const r = Math.random() * 16 | 0;
                const v = c == 'x' ? r : (r & 0x3 | 0x8);
                return v.toString(16);
            });
        }
        localStorage.setItem('paywall_client_id', clientId);
    }
    return clientId;
}


// 倒计时函数
function startCountdown(contentId, expireTime) {
    console.log('开始倒计时:', contentId, expireTime); // 调试日志

    const countdownEl = document.querySelector(`#countdown-${contentId}`);
    if (!countdownEl) {
        console.error('找不到倒计时元素');
        return;
    }

    const modalEl = document.getElementById(`payment-modal-${contentId}`);
    if (modalEl && modalEl.dataset.timerId) {
        clearInterval(Number(modalEl.dataset.timerId));
    }

        // 立即执行一次
        updateCountdown(contentId, expireTime);
    
        // 设置定时器，每秒更新一次
    const timer = setInterval(() => updateCountdown(contentId, expireTime), 1000);
    
        // 保存timer id
        if (modalEl) {
            modalEl.dataset.timerId = timer.toString();
            console.log('保存定时器ID:', timer); // 调试日志
        }

}

function updateCountdown(contentId, expireTime) {
    const countdownEl = document.querySelector(`#countdown-${contentId}`);
    if (!countdownEl) {
        console.error('找不到倒计时元素');
        return;
    }

    const now = Date.now();
    const timeLeft = Math.max(0, parseInt(expireTime) - now);
    
    console.log('剩余时间(毫秒):', timeLeft); // 调试日志

    if (timeLeft <= 0) {
        countdownEl.textContent = "支付已超时";
        countdownEl.classList.add('expired');
        closePaymentModal(contentId);
        alert('支付超时，请重新发起支付');
        return;
    }

    // 计算分钟和秒数
    const minutes = Math.floor(timeLeft / (1000 * 60));
    const seconds = Math.floor((timeLeft % (1000 * 60)) / 1000);
    
    // 更新倒计时显示
    const timeString = `支付倒计时：${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
    console.log('更新倒计时显示:', timeString); // 调试日志
    countdownEl.textContent = timeString;

    
}


