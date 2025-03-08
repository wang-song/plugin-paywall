// 创建支付弹窗
function createPaymentModal(contentId, price) {
    const modal = document.createElement('div');
    modal.className = 'payment-modal';
    modal.id = `payment-modal-${contentId}`;
    
    modal.innerHTML = `
        <div class="payment-modal-content">
            <button class="payment-close" onclick="closePaymentModal('${contentId}')">&times;</button>
            <div class="payment-title">购买付费内容</div>
            <div class="payment-amount">¥${price}</div>
            <div class="payment-qrcode" id="qrcode-${contentId}">
                <img src="" alt="支付二维码" id="qrcode-img-${contentId}">
            </div>
            <div class="payment-status" id="payment-status-${contentId}">正在等待支付...</div>
        </div>
    `;
    
    document.body.appendChild(modal);
    return modal;
}

// 关闭支付弹窗
function closePaymentModal(contentId) {
    const modal = document.getElementById(`payment-modal-${contentId}`);
    if (modal) {
        modal.remove();
    }
}

// 更新支付状态显示
function updatePaymentStatus(contentId, message) {
    const statusElement = document.getElementById(`payment-status-${contentId}`);
    if (statusElement) {
        statusElement.textContent = message;
    }
}

// 显示付费内容
function showPaywallContent(contentId) {
    const container = document.querySelector(`#content-${contentId}`);
    if (container) {
        container.innerHTML = data.content;
        container.style.display = 'block';
        
        // 隐藏支付区域
        const paymentArea = container.parentElement.querySelector('.paywall-payment-area');
        if (paymentArea) paymentArea.style.display = 'none';
        
        // 隐藏预览区域
        const previewArea = container.parentElement.querySelector('.paywall-preview');
        if (previewArea) previewArea.style.display = 'none';
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

        const url = `/apis/plugin-paywall.halo.run/v1alpha1/paywall/purchase/${clientAndContentString}`;


        console.log('url:', url);
        const response = await fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        });
        
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
        
        // 开始轮询支付状态
        const checkPaymentStatus = async () => {
            try {
                const statusResponse = await fetch(`/apis/plugin-paywall.halo.run/v1alpha1/paywall/purchase/status/${contentId}`);
                if (!statusResponse.ok) {
                    throw new Error('检查支付状态失败');
                }
                
                const statusData = await statusResponse.json();
                
                if (statusData.payStatus === 'SUCCESS') {
                    // 支付成功
                    updatePaymentStatus(contentId, '支付成功！正在加载内容...');
                    setTimeout(() => {
                        closePaymentModal(contentId);
                        // 获取内容
                        fetchContent(contentId);
                    }, 1500);
                    return;
                } else if (statusData.payStatus === 'FAILED') {
                    updatePaymentStatus(contentId, '支付失败，请重试');
                    setTimeout(() => {
                        closePaymentModal(contentId);
                    }, 2000);
                    return;
                }
                
                // 继续轮询
                updatePaymentStatus(contentId, '正在等待支付...');
                setTimeout(checkPaymentStatus, 2000);
            } catch (error) {
                console.error('检查支付状态失败:', error);
                updatePaymentStatus(contentId, '检查支付状态失败，请刷新页面重试');
            }
        };
        
        // 开始检查支付状态
        checkPaymentStatus();

    } catch (error) {
        console.error('处理支付失败:', error);
        updatePaymentStatus(contentId, '创建支付订单失败，请重试');
        setTimeout(() => {
            closePaymentModal(contentId);
        }, 2000);
    }
}

// 获取付费内容
function fetchContent(contentId) {
    fetch(`/apis/plugin-paywall.halo.run/v1alpha1/paywall/content/${contentId}`)
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

// 页面加载完成后检查已购买的内容
document.addEventListener('DOMContentLoaded', async () => {
    try {
        const response = await fetch('/apis/api.plugin.halo.run/v1alpha1/plugins/plugin-paywall/purchased-contents');
        if (!response.ok) {
            throw new Error('获取已购买内容失败');
        }
        
        const purchasedContents = await response.json();
        
        // 显示已购买的内容
        purchasedContents.forEach(contentId => {
            showPaywallContent(contentId);
        });
    } catch (error) {
        console.error('获取已购买内容失败:', error);
    }
});

// 处理付费内容的显示
(function() {
    // 查找所有付费内容区块
    function initPaywall() {
        const paywallElements = document.querySelectorAll('[data-paywall]');
        paywallElements.forEach(element => {
            const price = element.getAttribute('data-price');
            const preview = element.getAttribute('data-preview');
            const contentId = element.getAttribute('data-content-id');
            
            // 创建付费内容包装器
            const wrapper = document.createElement('div');
            wrapper.className = 'paywall-wrapper';
            wrapper.innerHTML = `
                <div class="paywall-header">
                    <div class="paywall-price">
                        <span class="price-label">付费内容</span>
                        <span class="price-amount">¥${price}</span>
                    </div>
                    ${preview ? `<div class="paywall-preview">${preview}</div>` : ''}
                </div>
                <div class="paywall-actions">
                    <button class="purchase-button" onclick="handlePurchase('${contentId}')">
                        立即购买
                    </button>
                </div>
            `;
            
            // 保存原始内容
            const content = element.innerHTML;
            element.innerHTML = '';
            element.appendChild(wrapper);
            
            // 检查是否已购买
            checkPurchaseStatus(contentId).then(purchased => {
                if (purchased) {
                    wrapper.innerHTML = content;
                }
            });
        });
    }
    
    // 检查购买状态
    async function checkPurchaseStatus(contentId) {
        try {
            const response = await fetch(`/apis/api.plugin.halo.run/v1alpha1/plugins/paywall/status/${contentId}`);
            if (response.ok) {
                const data = await response.json();
                return data.purchased;
            }
        } catch (error) {
            console.error('Failed to check purchase status:', error);
        }
        return false;
    }
    
    // 页面加载完成后初始化
    document.addEventListener('DOMContentLoaded', initPaywall);
})();


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